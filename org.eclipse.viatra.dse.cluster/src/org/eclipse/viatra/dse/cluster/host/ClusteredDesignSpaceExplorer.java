package org.eclipse.viatra.dse.cluster.host;

import java.io.IOException;
import java.io.StringWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.incquery.runtime.exception.IncQueryException;
import org.eclipse.viatra.dse.api.DSEException;
import org.eclipse.viatra.dse.api.DSETransformationRule;
import org.eclipse.viatra.dse.api.DesignSpaceExplorer;
import org.eclipse.viatra.dse.api.strategy.interfaces.IStrategy;
import org.eclipse.viatra.dse.base.GlobalContext;
import org.eclipse.viatra.dse.cluster.IDesignSpaceChange;
import org.eclipse.viatra.dse.cluster.interfaces.IGlobalConstraintFactory;
import org.eclipse.viatra.dse.cluster.interfaces.IObjectiveFactory;
import org.eclipse.viatra.dse.cluster.interfaces.IProblemServer;
import org.eclipse.viatra.dse.cluster.interfaces.IProblemServerPrivate;
import org.eclipse.viatra.dse.cluster.interfaces.IProcessingClient;
import org.eclipse.viatra.dse.cluster.interfaces.IRemoteDesignSpace;
import org.eclipse.viatra.dse.cluster.interfaces.IStrategyFactory;
import org.eclipse.viatra.dse.cluster.interfaces.ITransformationRuleFactory;
import org.eclipse.viatra.dse.designspace.api.IDesignSpace;
import org.eclipse.viatra.dse.statecode.IStateCoderFactory;
import org.eclipse.viatra.dse.util.EMFHelper;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;

import akka.actor.ActorSystem;
import akka.actor.TypedActor;
import akka.actor.TypedProps;
import akka.japi.Creator;

public class ClusteredDesignSpaceExplorer {

	private final ClusterConfig config;

	public Integer fixedThreads = null;

	private static final Logger logger = Logger.getLogger(ClusteredDesignSpaceExplorer.class.getName());

	private static final long SLEEP_INTERVAL = 1000;

	private final ActorSystem system;

	final IProblemServerPrivate problemHostActor;

	private final IRemoteDesignSpace designSpaceActor;

	private final List<String> nodes = new ArrayList<String>();

	private final ClusteredDesignSpaceExplorer self = this;

	private final Set<EPackage> metaModelPackages = new HashSet<EPackage>();
	private final List<ClassLoader> registeredClassloaders = new ArrayList<ClassLoader>();

	private final List<String> transformationRuleFactories = new ArrayList<String>();

	private final List<String> objectiveFactories = new ArrayList<String>();
	private final List<String> goalPatternFactories = new ArrayList<String>();
	private final List<String> constraintFactories = new ArrayList<String>();

	private final GlobalContext globalContext = new GlobalContext();

	private String modelCloneAsXMI;

	private String strategyName;

	/**
	 * Starts the design space exploration. If {@code waitForTermination} is
	 * true, then it returns only when the strategy decides to stop the
	 * execution, otherwise when the exploration process is started it returns
	 * immediately. In this case, process completion can be verified by calling
	 * {@link DesignSpaceExplorer#isDone()}.
	 * 
	 * @param strategyBase
	 *            The strategy of the exploration.
	 * @param waitForTermination
	 *            True if the method must wait for the engine to stop.
	 * @throws DSEException
	 *             On any execution error, a {@link DSEException} is thrown.
	 *             It's is a descendant of {@link RuntimeException}, so it may
	 *             be left unchecked.
	 */
	public void startExploration(IStrategyFactory strategyBase, boolean waitForTermination) throws DSEException {
		initExploration(strategyBase);

		long processStartTime = System.currentTimeMillis();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}

		long lastCheck = Long.MAX_VALUE;
		long lastDSStateSize = 0;
		long lastDSTransitionSize = 0;
		long lastDSFiredTransitionSize = 0;

		// wait until all threads exit
		while (waitForTermination) {
			// this map will hold the states of the specific nodes
			Map<String, IProcessingClient.ExplorationNodeState> nodeStates = new HashMap<String, IProcessingClient.ExplorationNodeState>();
			for (String nodeAddress : problemHostActor.getNodeAddresses()) {
				IProcessingClient.ExplorationNodeState nodeState;
				try {
					nodeState = problemHostActor.getNodeState(nodeAddress);
				} catch (TimeoutException e) {
					logger.warn("Node at " + nodeAddress + " had timed out.");
					nodeState = IProcessingClient.ExplorationNodeState.NOT_RESPONDING;
				}
				nodeStates.put(nodeAddress, nodeState);
			}

			// we reverse the reference, so that we have the list of nodes that
			// are working, not responding, completed etc.
			Map<IProcessingClient.ExplorationNodeState, List<String>> nodesByState = new HashMap<IProcessingClient.ExplorationNodeState, List<String>>();
			for (IProcessingClient.ExplorationNodeState stateType : IProcessingClient.ExplorationNodeState.values()) {
				nodesByState.put(stateType, new ArrayList<String>());
			}
			for (String node : nodeStates.keySet()) {
				IProcessingClient.ExplorationNodeState entry = nodeStates.get(node);
				nodesByState.get(entry).add(node);
			}

			// processing has been completed, all nodes stopped processing
			if (nodesByState.get(IProcessingClient.ExplorationNodeState.COMPLETED)
					.size() == (problemHostActor.getNodeAddresses().size()
							- nodesByState.get(IProcessingClient.ExplorationNodeState.NOT_RESPONDING).size())) {
				// but only if the designspace has no more workable states!
				if (designSpaceActor.getWorkableStates().isEmpty()) {
					logger.info("DesignSpaceExplorer finished.");
					break;
				}
			}

			long now = System.nanoTime();
			long nowDSStateSize = designSpaceActor.getNumberOfStates();
			long nowDSTransitionSize = designSpaceActor.getNumberOfTransitions();
			long nowDSFiredTransitionSize = designSpaceActor.getNumberOfFiredTransitions();


			String nodeStateText;
			String dsStateText;

			nodeStateText = nodesByState.get(IProcessingClient.ExplorationNodeState.PROCESSING).size() + " of "
					+ nodeStates.size() + " cluster nodes processing. ";
			dsStateText = "Current DS size: " + nowDSStateSize + " Transitions (fired/total): "
					+ nowDSFiredTransitionSize + "/" + nowDSTransitionSize;

			if (lastCheck < now) {
				long elapsed = now - lastCheck;
				double elapsedMS = TimeUnit.MILLISECONDS.convert(elapsed, TimeUnit.NANOSECONDS);
				double dsStateSpeed = (((double) (nowDSStateSize - lastDSStateSize)) / elapsedMS) * 1000;
				DecimalFormat twoDecimal = new DecimalFormat("#.##");
				twoDecimal.setRoundingMode(RoundingMode.HALF_UP);

				double dsTransitionSpeed = ((double) (nowDSTransitionSize - lastDSTransitionSize) / elapsedMS) * 1000;
				double dsFiredTransitionSpeed = ((double) (nowDSFiredTransitionSize - lastDSFiredTransitionSize)
						/ elapsedMS) * 1000;

				dsStateText += " " + twoDecimal.format(dsStateSpeed) + " state/sec";
				dsStateText += " " + twoDecimal.format(dsFiredTransitionSpeed) + "/"
						+ twoDecimal.format(dsTransitionSpeed) + " /sec";
			}

			lastCheck = now;
			lastDSStateSize = nowDSStateSize;
			lastDSTransitionSize = nowDSTransitionSize;
			lastDSFiredTransitionSize = nowDSFiredTransitionSize;

			logger.info(nodeStateText + dsStateText);

			// if there are still working nodes
			if (!nodesByState.get(IProcessingClient.ExplorationNodeState.COMPLETED).isEmpty()) {
				for (String completedNode : nodesByState.get(IProcessingClient.ExplorationNodeState.COMPLETED)) {
					logger.info("Attempting to restart node '" + completedNode + "'...");
					List<String> workableStates = designSpaceActor.getWorkableStates();
					if (workableStates.isEmpty()) {
						logger.warn("No workable states in the DS.");
						String workingNode = nodesByState.get(IProcessingClient.ExplorationNodeState.PROCESSING).get(0);
						if (problemHostActor.requestWorkableStatesFrom(workingNode)) {
							logger.info("Workable state request sent to " + workingNode);
							break;
						}
					} else {
						problemHostActor.restartNode(completedNode, workableStates.remove(0));
						logger.info("Node " + completedNode + " restarted.");
						break;
					}
				}
			}
			try {
				Thread.sleep(SLEEP_INTERVAL);
			} catch (InterruptedException e) {
			}
		}

		long processFinish = System.currentTimeMillis();

		// designSpaceExporter.exportDesignSpace(getGlobalContext().getDesignSpace());


		system.shutdown();
		system.awaitTermination();

		logger.info("Actor system terminated: " + system.isTerminated());
		logger.info("Design Space exploration finished in " + (processFinish - processStartTime) + " ms");

		// logger.info("DesignSpaceExplorer working in detached mode.");
	}

	/**
	 * Starts the design space exploration and then sleeps
	 * {@code waitInMilliseconds} millisecond. After that it stops the execution
	 * which can be a few millis long and returns.
	 * 
	 * @param strategyBase
	 *            The strategy of the exploration.
	 * @param waitInMilliseconds
	 *            The number of milliseconds the method must wait for stopping
	 *            the exploration.
	 * @throws DSEException
	 *             On any execution error, a {@link DSEException} is thrown.
	 *             It's is a descendant of {@link RuntimeException}, so it may
	 *             be left unchecked.
	 */
	public void startExploration(IStrategyFactory strategyBase, int waitInMilliseconds) throws DSEException {
		initExploration(strategyBase);

		try {
			Thread.sleep(waitInMilliseconds);
		} catch (InterruptedException e) {
		}

		logger.info("Stopping threads...");

		getGlobalContext().stopAllThreads();

		// wait until all threads exit
		do {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
			}

			if (getGlobalContext().isDone()) {
				logger.info("DesignSpaceExplorer finished.");
				return;
			}
		} while (true);
	}

	public List<ClassLoader> getRegisteredClassloaders() {
		return registeredClassloaders;
	}

	public ClusteredDesignSpaceExplorer(List<String> nodes) {
		this(nodes, ClusterConfig.defaultConfig());
	}

	public ClusteredDesignSpaceExplorer(List<String> nodes, ClusterConfig config) {
		this.config = config;

		ClassLoader clazz = this.getClass().getClassLoader();

		Config appConfig = ConfigFactory.load(clazz, "application.conf",
				ConfigParseOptions.defaults().setAllowMissing(false), ConfigResolveOptions.defaults());
		Config referenceActorConfig = ConfigFactory.load(clazz, "reference-actor.conf",
				ConfigParseOptions.defaults().setAllowMissing(false), ConfigResolveOptions.defaults());
		Config referenceRemoteConfig = ConfigFactory.load(clazz, "reference-remote.conf",
				ConfigParseOptions.defaults().setAllowMissing(false), ConfigResolveOptions.defaults());
		appConfig = appConfig.withFallback(referenceRemoteConfig).withFallback(referenceActorConfig);

		Config debugConfig = ConfigFactory.parseString("akka.loglevel = \"DEBUG\"");
		Config portConfig = ConfigFactory.parseString("akka.remote.netty.tcp.port = " + config.getServicePort());
		Config hostConfig = ConfigFactory.parseString("akka.remote.netty.tcp.hostname = " + config.getServiceIP());

		appConfig = hostConfig.withFallback(portConfig).withFallback(debugConfig).withFallback(appConfig);

		// create ActorSystem
		system = ActorSystem.create(IProblemServer.ACTOR_SYSTEM_NAME, appConfig, this.getClass().getClassLoader());

		this.nodes.addAll(nodes);

		problemHostActor = TypedActor.get(system)
				.typedActorOf(new TypedProps<Server>(IProblemServerPrivate.class, new Creator<Server>() {
					private static final long serialVersionUID = 1L;

					public Server create() {
						return new Server(self, system, ClusteredDesignSpaceExplorer.this.config.getServiceIP(),
								ClusteredDesignSpaceExplorer.this.config.getServicePort());
					}
				}), IProblemServerPrivate.REMOTE_CLASSLOADER_HOST_NAME);
		designSpaceActor = TypedActor.get(system).typedActorOf(
				new TypedProps<RemoteDesignSpaceActor>(IRemoteDesignSpace.class, new Creator<RemoteDesignSpaceActor>() {
					private static final long serialVersionUID = 1L;

					public RemoteDesignSpaceActor create() {
						return new RemoteDesignSpaceActor(self, system);
					}
				}), IProblemServerPrivate.REMOTE_DESIGNSPACE_HOST_NAME);
	}

	/**
	 * This method is required to transmit configuration information to remote
	 * nodes.
	 * 
	 * @return the classname of the strategy to be used.
	 */
	public String getStrategyName() {
		return strategyName;
	}

	/**
	 * This method starts the problem node that is able to delegate calculation
	 * to processing nodes.
	 * 
	 * @param strategyBase
	 *            the {@link IStrategy} class to be used.
	 */
	protected void initExploration(IStrategyFactory strategyBase) {
		// super.initExploration(strategyBase);
		strategyName = strategyBase.getClass().getName();
		addClassLoader(strategyBase.getClass().getClassLoader());

		problemHostActor.startClustering();
	}

	public List<String> getNodes() {
		return nodes;
	}

	public IProblemServerPrivate getHost() {
		return problemHostActor;
	}

	public Set<String> getRemoteMetamodelPackages() {
		Set<String> metaModelPackages = new HashSet<String>();

		for (EPackage pck : getMetaModelPackages()) {
			metaModelPackages.add(pck.getClass().getCanonicalName());
		}

		return metaModelPackages;
	}

	public void setInitialModel(EObject rootEObject, boolean deepCopyModel) {
		// setInitialModel(rootEObject, deepCopyModel);
		// EMFHelper.serializeModel(rootEObject, name, ext);

		// get the Model's classloader
		ClassLoader modelClassloader = rootEObject.getClass().getClassLoader();

		// and register, so that it can be used later by the remote classloading
		// provider
		if (!registeredClassloaders.contains(modelClassloader)) {
			registeredClassloaders.add(modelClassloader);
		}

		// serilized the model to be sent to the nodes
		modelCloneAsXMI = saveModelToXMIString(EMFHelper.clone(rootEObject));
	}

	public void setInitialModel(EObject rootEObject) {
		setInitialModel(rootEObject, true);
	}

	public String getModelClone() {
		return modelCloneAsXMI;
	}

	private String saveModelToXMIString(EObject model) {
		try {
			ResourceSet resourceSet = new ResourceSetImpl();
			Resource resource = resourceSet.createResource(URI.createURI("http:///My.library"));

			resource.getContents().add(model);

			StringWriter stringWriter = new StringWriter();
			resource.save(new URIConverter.WriteableOutputStream(stringWriter, "UTF-8"), null);
			String xmiString = stringWriter.toString();
			//
			// Resource resource2 =
			// resourceSet.createResource(URI.createURI("http:///My2.library"));
			// resource2.load(new URIConverter.ReadableInputStream(xmiString),
			// null);
			//
			// resource.save(System.out, null);

			return xmiString;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void addClassLoader(ClassLoader cl) {
		if (!registeredClassloaders.contains(cl)) {
			registeredClassloaders.add(cl);
		}
	}

	public void registerTransformationRuleFactory(ITransformationRuleFactory factory) throws IncQueryException {
		// put the factory classname in a serialiseable container
		transformationRuleFactories.add(factory.getClass().getName());

		// register the appropriate classloaders
		addClassLoader(factory.getClass().getClassLoader());

		DSETransformationRule<?, ?> rule = factory.create();

		ruleMap.put(rule.getName(), rule);
	}

	public List<String> getTransformationRuleFactories() {
		return transformationRuleFactories;
	}

	public void registerObjectiveFactory(IObjectiveFactory objectiveFactory) {
		// put the factory classname in a serialiseable container
		objectiveFactories.add(objectiveFactory.getClass().getName());

		// register the appropriate classloaders
		addClassLoader(objectiveFactory.getClass().getClassLoader());
	}

	public List<String> getGoalPatternDefinitions() {
		return goalPatternFactories;
	}

	public void registerGlobalConstraintFactory(IGlobalConstraintFactory constraint) {
		// put the factory classname in a serialiseable container
		constraintFactories.add(constraint.getClass().getName());

		// register the appropriate classloaders
		addClassLoader(constraint.getClass().getClassLoader());
	}

	public List<String> getConstraintFactories() {
		return constraintFactories;
	}


	public List<String> getObjectiveFactories() {
		return objectiveFactories;
	}

	public void addMetaModelPackage(EPackage metaModelPackage) {
		metaModelPackages.add(metaModelPackage);
	}

	public Set<EPackage> getMetaModelPackages() {
		return new HashSet<EPackage>(metaModelPackages);
	}

	public void setStateCoderFactory(IStateCoderFactory stateCoderFactory) {
		addClassLoader(stateCoderFactory.getClass().getClassLoader());
		globalContext.setStateCoderFactory(stateCoderFactory);
	}

	// public void setGuidance(Guidance guidance) {
	// throw new UnsupportedOperationException();
	// }
	//
	// public void setPredicatesForOcVectorResolving(List<Predicate> predicates)
	// {
	// throw new UnsupportedOperationException();
	// }
	//
	public void setDesignspace(IDesignSpace designspace) {
		globalContext.setDesignSpace(designspace);
	}

	// @Override
	// public void setSolutionStore(ISolutionStore solutionStore) {
	// throw new UnsupportedOperationException();
	// }

	public void startExploration(IStrategyFactory strategyFactory) {
		initExploration(strategyFactory);
	}

	// public void startExplorationAsync(IStrategy strategy) {
	// throw new UnsupportedOperationException();
	// }
	//
	// @Override
	// public boolean startExplorationWithTimeout(IStrategy strategy, long
	// timeout) {
	// throw new UnsupportedOperationException();
	// }
	//
	// @Override
	// public boolean startExplorationAsyncWithTimeout(IStrategy strategy, long
	// timeout) {
	// throw new UnsupportedOperationException();
	// }

	// @Override
	// public boolean startExploration(IStrategy strategy, boolean
	// waitForTermination, long timeout) {
	// throw new UnsupportedOperationException();
	//
	// }

	// @Override
	// public Collection<Solution> getSolutions() {
	// throw new UnsupportedOperationException();
	//
	// }
	//
	// @Override
	// public SolutionTrajectory getArbitrarySolution() {
	// throw new UnsupportedOperationException();
	//
	// }
	//
	// @Override
	// public boolean isDone() {
	// throw new UnsupportedOperationException();
	//
	// }

	private final Map<String, DSETransformationRule<?, ?>> ruleMap = new HashMap<String, DSETransformationRule<?, ?>>();

	DSETransformationRule<?, ?> convertToLocalRule(String ruleName) {
		DSETransformationRule<?, ?> dseTransformationRule = ruleMap.get(ruleName);
		if (dseTransformationRule == null) {
			throw new DSEException("No rule has been defined with the name " + ruleName);
		}
		return dseTransformationRule;
	}

	public GlobalContext getGlobalContext() {
		return globalContext;
	}

	public IProblemServerPrivate getServer() {
		return problemHostActor;
	}

	public void send(List<IDesignSpaceChange> changes, String targetNode) {
		throw new UnsupportedOperationException();
	}
}
