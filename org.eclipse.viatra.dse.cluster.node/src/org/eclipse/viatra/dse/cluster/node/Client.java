package org.eclipse.viatra.dse.cluster.node;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.viatra.dse.api.DesignSpaceExplorer;
import org.eclipse.viatra.dse.cluster.interfaces.IProblemServer;
import org.eclipse.viatra.dse.cluster.interfaces.IProcessingClient;
import org.eclipse.viatra.dse.cluster.interfaces.IRemoteDesignSpace;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.TypedActor;
import akka.actor.TypedProps;

public class Client implements IProcessingClient {

	private static Logger log = Logger.getLogger(Client.class.getName());

	private final ActorSystem system;

	private Integer fixedThreads = null;

	private final Map<String, DesignSpaceExplorer> explorers = new HashMap<String, DesignSpaceExplorer>();
	private final Map<String, IProblemServer> explorerActors = new HashMap<String, IProblemServer>();
	private final Map<String, IRemoteDesignSpace> designSpaceActors = new HashMap<String, IRemoteDesignSpace>();

	public Map<String, DesignSpaceExplorer> getExplorers() {
		return explorers;
	}

	public Map<String, IProblemServer> getExplorerActors() {
		return explorerActors;
	}

	public Map<String, IRemoteDesignSpace> getDesignSpaceActors() {
		return designSpaceActors;
	}

	private RemoteAkkaClassloader customCL;

	/**
	 * Initialise the Node around the supplied AKKA ActorSystem
	 * 
	 * @param system
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	public Client(ActorSystem system) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		this.system = system;

		// hack the custom classloader into the classloading chain by redefining
		// the system classloader

		// get the reflection object for the SCL field
		Field scl = ClassLoader.class.getDeclaredField("scl");
		// remove read/write protection
		scl.setAccessible(true);
		// read it
		ClassLoader systemCL = (ClassLoader) scl.get(null);
		// Update it to the custom made classloader which has the SCL as parent
		scl.set(null, new RemoteAkkaClassloader(systemCL));

		// keep a reference to it just in case
		customCL = (RemoteAkkaClassloader) ClassLoader.getSystemClassLoader();

		// if the cast was successful then the hacking has been successfull		
		log.info("Class loader hack successfull! CustomCL:" + customCL + " SCL: " + systemCL);

		// register XMI factory for XMI serialisation
		Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
		Map<String, Object> m = reg.getExtensionToFactoryMap();
		m.put("*", new XMIResourceFactoryImpl());
	}

	/**
	 * Entry point for a new work.
	 */
	@Override
	public void submitProblem(String recallAddress, String designSpaceAddress, String initiatorClass, String initalStateXMI) {
		log.info("Problem received. Recall address: " + recallAddress + " Init class: " + initiatorClass);

		// get local actorRef
		ActorRef ref = system.actorFor(recallAddress);
		// get local typed actor
		IProblemServer hostActor = TypedActor.get(system).typedActorOf(new TypedProps<IProblemServer>(IProblemServer.class), ref);
		// register the addresses
		explorerActors.put(recallAddress, hostActor);

		// get local ActorRef of designSpace
		ActorRef refDS = system.actorFor(designSpaceAddress);
		IRemoteDesignSpace remoteDesignSpace = TypedActor.get(system).typedActorOf(new TypedProps<IRemoteDesignSpace>(IRemoteDesignSpace.class), refDS);
		designSpaceActors.put(recallAddress, remoteDesignSpace);

		// set up the custom CL to use this actor as the remote CL source
		customCL.setRemoteClassLoaderActor(hostActor);
		String reply = hostActor.ping();

		try {
			// create the initiator object
			DesignSpaceExplorer instance = (DesignSpaceExplorer) Class.forName(initiatorClass).newInstance();

			// register initiator
			explorers.put(recallAddress, instance);

			log.info("Intantiated initiator class named: " + instance.getClass().getName());

			ClientConfigurator configurator = new ClientConfigurator(this, recallAddress, initalStateXMI);
			configurator.setFixedThreads(fixedThreads);
			new Thread(configurator).start();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}

	@Override
	public ExplorationNodeState getNodeState(String address) {
		DesignSpaceExplorer designSpaceExplorer = explorers.get(address);
		if (designSpaceExplorer == null || designSpaceExplorer.getGlobalContext() == null) {
			return ExplorationNodeState.WAITING_FOR_TASK;
		}

		switch (designSpaceExplorer.getGlobalContext().getState()) {
		case NOT_STARTED:
			return ExplorationNodeState.TASK_RECEIVED;
		case WAITING_FOR_NEW_ROOT:
			return ExplorationNodeState.WAITING_FOR_NEW_ROOT;
		case RUNNING:
		case STOPPING:
			return ExplorationNodeState.PROCESSING;
		case COMPLETED:
			return ExplorationNodeState.COMPLETED;
		}

		return ExplorationNodeState.COMPLETED;
	}

	@Override
	public void requestWorkableState(String recallAddress) {
		DesignSpaceExplorer designSpaceExplorer = explorers.get(recallAddress);
		designSpaceExplorer.getGlobalContext().requestWorkableState();
	}

	@Override
	public void fixThreads(int threadCount) {
		fixedThreads = threadCount;
	}
}
