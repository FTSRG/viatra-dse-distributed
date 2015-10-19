package org.eclipse.viatra.dse.cluster.node;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EPackage.Registry;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.URIConverter.ReadableInputStream;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.incquery.runtime.api.IMatchProcessor;
import org.eclipse.incquery.runtime.api.IPatternMatch;
import org.eclipse.incquery.runtime.api.IQuerySpecification;
import org.eclipse.incquery.runtime.api.IncQueryMatcher;
import org.eclipse.incquery.runtime.exception.IncQueryException;
import org.eclipse.viatra.dse.api.DSETransformationRule;
import org.eclipse.viatra.dse.api.DesignSpaceExplorer;
import org.eclipse.viatra.dse.api.PatternWithCardinality;
import org.eclipse.viatra.dse.api.strategy.interfaces.IStrategy;
import org.eclipse.viatra.dse.cluster.interfaces.IObjectiveFactory;
import org.eclipse.viatra.dse.cluster.interfaces.IProblemServer;
import org.eclipse.viatra.dse.cluster.interfaces.IStrategyFactory;
import org.eclipse.viatra.dse.cluster.interfaces.ITransformationRuleFactory;
import org.eclipse.viatra.dse.objectives.IObjective;
import org.eclipse.viatra.dse.statecode.IStateCoderFactory;
import org.eclipse.viatra.dse.statecode.graph.GraphHasherFactory;
import org.eclipse.viatra.dse.statecode.incrementalgraph.IncrementalGraphHasherFactory;

public class ClientConfigurator implements Runnable {

	private Integer fixedThreads = null;

	public void setFixedThreads(Integer fixedThreads) {
		this.fixedThreads = fixedThreads;
	}

	private static final int EXTRA_WORKER_THREADS = 2;

	private static Logger log = Logger.getLogger(ClientConfigurator.class.getName());

	private final Client node;

	private final String recallAddress;

	private final String initialModelXMI;

	/**
	 * This class configures the local DSE instance to execute the job defined
	 * by the host that is reachable on the specified Akka recall address.
	 * 
	 * @param node
	 *            The local node.
	 * @param recallAddress
	 *            the remote Akka recall address
	 */
	public ClientConfigurator(Client node, String recallAddress, String initialModelXMI) {
		super();
		this.node = node;
		this.recallAddress = recallAddress;
		this.initialModelXMI = initialModelXMI;
	}

	private DesignSpaceExplorer dse;

	@Override
	public void run() {

		try {
			dse = configure(recallAddress);

			// IDSEProblemHost host =
			// node.getExplorerActors().get(recallAddress);
			// String ping = host.ping();

			log.debug("The dse task from the addess " + recallAddress
					+ " that was set up with the DSEJobConfigurator has finished.");

			// System.out.println("Number of states: " +
			// dse.getNumberOfStates());
			// System.out.println("Number of transitions: " +
			// dse.getNumberOfTransitions());
			// System.out.println("Number of deadlocks (solutions): " +
			// dse.getAllSolutions().size());
		} catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException | NoSuchFieldException | InstantiationException
				| IOException e) {
			e.printStackTrace();
		} catch (IncQueryException e) {
			e.printStackTrace();
		}

	}

	/**
	 * This method downloads all the relevant configuration information from the
	 * host, and then starts the execution of the exploration.
	 * 
	 * @param recallAddress
	 * @return
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws IncQueryException
	 */
	private DesignSpaceExplorer configure(String recallAddress) throws ClassNotFoundException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException,
			NoSuchFieldException, IOException, InstantiationException, IncQueryException {

		// get the custom classloader (which is the System classloader)
		ClassLoader cl = ClassLoader.getSystemClassLoader();

		log.info("Configuring " + node.getExplorers().get(recallAddress).getClass().getCanonicalName() + " from "
				+ recallAddress);

		// get relevant object references
		dse = (DesignSpaceExplorer) node.getExplorers().get(recallAddress);
		dse.setDesignspace(new RemoteDesignSpace(node.getDesignSpaceActors().get(recallAddress)));

		int maxThreads = getNumberOfProcessors() + EXTRA_WORKER_THREADS;
		maxThreads /= 2;
		 maxThreads = 6;
		if (fixedThreads != null) {
			dse.setMaxNumberOfThreads(fixedThreads);
			log.info("Configuring max number of threads: " + fixedThreads + " (HOST OVERRIDE)");
		} else {
			dse.setMaxNumberOfThreads(maxThreads);
			log.info("Configuring max number of threads: " + maxThreads + " (Core count: " + getNumberOfProcessors()
					+ " Extra: " + EXTRA_WORKER_THREADS + ")");
		}

		IProblemServer host = node.getExplorerActors().get(recallAddress);

		// configure those things that cannot be simply serialised
		// configure EPackages
		Set<EPackage> packages = new HashSet<EPackage>();
		Set<String> names = host.getRemoteMetamodelPackages();
		for (String pckName : names) {
			if (pckName.endsWith("Impl")) {
				pckName = pckName.replace("Impl", "");
				pckName = pckName.replace(".impl", "");
			}
			Class<?> pck = cl.loadClass(pckName);
			Field field = pck.getField("eINSTANCE");
			EPackage pckInstance = (EPackage) field.get(null);
			packages.add(pckInstance);
		}

		for (EPackage pck : packages) {
			dse.addMetaModelPackage(pck);
		}

		// configure initial model
		EObject model;
		if (initialModelXMI == null) {
			model = xmiToModel(host.getModelXMI());
		} else {
			model = xmiToModel(initialModelXMI);
		}
		dse.setInitialModel(model, true);

		// configure state coder
		Class<?> stateCoderFactoryClass = cl.loadClass(host.getStateCoderFactory());

		if (stateCoderFactoryClass.getName().equals(IncrementalGraphHasherFactory.class.getName())) {
			dse.setStateCoderFactory(new IncrementalGraphHasherFactory(dse.getMetaModelPackages()));
		} else if (stateCoderFactoryClass.getName().equals(GraphHasherFactory.class.getName())) {
			dse.setStateCoderFactory(new GraphHasherFactory());
		} else {		
			Constructor<?> constructor2 = stateCoderFactoryClass.getConstructor();
			IStateCoderFactory iStateCoderFactory = (IStateCoderFactory) constructor2.newInstance();
			dse.setStateCoderFactory(iStateCoderFactory);
		}
		
		
		// configure Transformation rules
		List<String> transformationRuleDefinitions = host.getTransformationRuleFactories();
		for (String def : transformationRuleDefinitions) {
			Class<?> ruleFactoryClass = cl.loadClass(def);
			Constructor<?> constructor = ruleFactoryClass.getConstructor();
			ITransformationRuleFactory factory = (ITransformationRuleFactory) constructor.newInstance();

			DSETransformationRule<?, ?> rule = factory.create();
			dse.addTransformationRule(rule);
			RemoteTransition.ruleMap.put(rule.getName(), rule);
		}

		// configure objectives
		List<String> objectives = host.getObjectiveFactories();
		for (String obj : objectives) {
			Class<?> objecttiveClass = cl.loadClass(obj);
			Constructor<?> constructor = objecttiveClass.getConstructor();
			IObjectiveFactory factory = (IObjectiveFactory) constructor.newInstance();

			dse.addObjective(factory.create(dse));
		}

		String strategyName = host.getStrategyName();
		Class<?> ruleFactoryClass = cl.loadClass(strategyName);
		Constructor<?> constructor = ruleFactoryClass.getConstructor();
		IStrategyFactory factory = (IStrategyFactory) constructor.newInstance();

		dse.startExploration(factory.create());

		System.out.println("Config done");

		return dse;

		//
		// // configure any additional options defined through the configuration
		// map
		// Map<String, List<String>> configurationMap =
		// host.getConfigurationMap();
		// for (String key : configurationMap.keySet()) {
		// // create the parameter list
		// List<Object> parameters = new ArrayList<>();
		//
		// // download the parameters
		// for (String value : configurationMap.get(key)) {
		// Method method = IDSEProblemHost.class.getMethod(value, null);
		// parameters.add(method.invoke(host));
		// }
		//
		// Object[] parameterArray = parameters.toArray(new
		// Object[parameters.size()]);
		//
		// List<Class<?>> types = new ArrayList();
		// for (Object o : parameters) {
		// types.add(o.getClass());
		// }
		//
		// Class<?>[] classes = types.toArray(new Class<?>[types.size()]);
		//
		// // apply to the initiator
		//
		// Method method = initiatorObject.getClass().getMethod(key, classes);
		// method.invoke(initiatorObject, parameterArray);
		// }

		// System.out.println("Exiting");
	}

	private EObject xmiToModel(String xmiString) throws IOException {

		ResourceSet resourceSet = new ResourceSetImpl();
		Map<String, Object> packageRegistry = resourceSet.getPackageRegistry();

		Resource resource = resourceSet.createResource(URI.createURI("http:///My2.library"));

		for (EPackage pck : dse.getMetaModelPackages()) {
			resource.getResourceSet().getPackageRegistry().put(pck.getNsURI(), pck);
		}

		Map<String, Object> extensionToFactoryMap = Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap();
		extensionToFactoryMap.put("xmi", new XMIResourceFactoryImpl());

		resource.load(new URIConverter.ReadableInputStream(xmiString), null);

		return resource.getAllContents().next();
	}

	private int getNumberOfProcessors() {
		return Runtime.getRuntime().availableProcessors();
	}

}
