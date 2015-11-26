package org.eclipse.viatra.dse.cluster.host;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.viatra.dse.api.DesignSpaceExplorer;
import org.eclipse.viatra.dse.cluster.interfaces.IProblemServer;
import org.eclipse.viatra.dse.cluster.interfaces.IProblemServerPrivate;
import org.eclipse.viatra.dse.cluster.interfaces.IProcessingClient;
import org.eclipse.viatra.dse.cluster.node.PatternDefinition;
import org.eclipse.viatra.dse.cluster.node.TransformationRuleDefinition;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.TypedActor;
import akka.actor.TypedProps;

public class Server implements IProblemServerPrivate {

	private final String serviceIP;
	private final String servicePort;

	private final String recallAddress;

	private final String dsAddress;

	private static Logger log = Logger.getLogger(Server.class.getName());

	private final ClusteredDesignSpaceExplorer cdse;

	private final ActorSystem system;

	Map<String, IProcessingClient> nodeRefs = new HashMap<String, IProcessingClient>();

	public Server(ClusteredDesignSpaceExplorer cdse, ActorSystem system, String serviceIP, String servicePort) {
		this.cdse = cdse;
		this.system = system;
		this.serviceIP = serviceIP;
		this.servicePort = servicePort;
		recallAddress = "akka.tcp://" + IProblemServer.ACTOR_SYSTEM_NAME + "@" + serviceIP + ":" + servicePort + "/user/"
				+ IProblemServer.REMOTE_CLASSLOADER_HOST_NAME;

		dsAddress = "akka.tcp://" + IProblemServer.ACTOR_SYSTEM_NAME + "@" + serviceIP + ":" + servicePort + "/user/"
				+ IProblemServer.REMOTE_DESIGNSPACE_HOST_NAME;
	}

	@Override
	public String getCoreClassname() {
		return DesignSpaceExplorer.class.getCanonicalName();
	}

	/**
	 * This method gets the contents code for a requested class and return sit
	 * as a byte array that can be loaded in any JVM later.
	 */
	@Override
	public byte[] loadClass(String className) throws Exception {
		return getClassDefByName(className);
	}

	/**
	 * Takes a classname and gets the resource for it, then extracts the
	 * contents and converts it into a byte array.
	 * 
	 * @param classname
	 *            the name of the class to be loaded.
	 * @return the class file's contents as a byte array.
	 */
	private byte[] getClassDefByName(String classname) throws Exception {

		byte[] bytecodeAsByte = null;

		for (ClassLoader cl : cdse.getRegisteredClassloaders()) {
			bytecodeAsByte = getClassDefByNameFrom(classname, cl);
			if (bytecodeAsByte != null) {
				return bytecodeAsByte;
			}
		}

		System.out.println("Failed to load requested class: " + classname);

		return null;
	}

	private byte[] getClassDefByNameFrom(String classname, ClassLoader cl) throws IOException {
		ClassLoader mcl = cl;

		String withDotsReplacedLeading = "/" + classname.replace(".", "/");
		String withDotsReplacedLeadingClass = withDotsReplacedLeading + ".class";

		InputStream asStream = null;
		asStream = mcl.getResourceAsStream(withDotsReplacedLeadingClass);

		if (asStream == null) {
			return null;
		} else {
			// convert it to bytes
			return toByteArray(asStream);
		}

	}

	private byte[] toByteArray(InputStream inputStream) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int read = 0;
		while ((read = inputStream.read(buffer, 0, buffer.length)) != -1) {
			baos.write(buffer, 0, read);
		}
		baos.flush();
		return baos.toByteArray();
	}

	@Override
	public void startClustering() {
		log.info("DSE SERVER Started");
		for (String nodeAddress : cdse.getNodes()) {
			System.out.println("Connecting node at " + nodeAddress);

			try {
				IProcessingClient node = contactNode(nodeAddress);
				if (node != null) {
					nodeRefs.put(nodeAddress, node);
					if (cdse.fixedThreads != null) {
						node.fixThreads(cdse.fixedThreads);
					}
					node.submitProblem(recallAddress, dsAddress, DesignSpaceExplorer.class.getCanonicalName(), null, nodeAddress);
				}
			} catch (Exception e) {
				log.error(e);
			}
		}
	}

	private IProcessingClient contactNode(String nodeAddress) {
		String selectAddress = "akka.tcp://" + IProblemServer.ACTOR_SYSTEM_NAME + "@" + nodeAddress + "/user/" + IProblemServer.REMOTE_NODE_NAME;

		ActorRef ref = system.actorFor(selectAddress);

		IProcessingClient nodeActor = TypedActor.get(system).typedActorOf(new TypedProps<IProcessingClient>(IProcessingClient.class), ref);

		return nodeActor;
	}

	@Override
	public String ping() {
		return "Ping Received at: " + new Date().toString();
	}

	@Override
	public Set<String> getRemoteMetamodelPackages() {
		Set<String> set = new HashSet<String>();
		for (EPackage pck : cdse.getMetaModelPackages()) {
			set.add(pck.getClass().getCanonicalName());
		}
		return set;
	}

	// @Override
	// public Map<String, List<String>> getStrategyConfigurationMap() {
	// return configurationMapping;
	// }

	@Override
	public String getModelXMI() {
		return cdse.getModelClone();
	}

	@Override
	public String getStrategyName() {
		return cdse.getStrategyName();
	}

	@Override
	public IProcessingClient.ExplorationNodeState getNodeState(String address) throws TimeoutException {
		try {
			IProcessingClient nodeProxy = nodeRefs.get(address);
			if (nodeProxy != null) {
				return nodeProxy.getNodeState(recallAddress);
			} else {
				return IProcessingClient.ExplorationNodeState.NOT_RESPONDING;
			}
		} catch (UndeclaredThrowableException e) {
			log.error(e);
			return IProcessingClient.ExplorationNodeState.NOT_RESPONDING;
		} catch (TimeoutException e) {
			log.error(e);
			return IProcessingClient.ExplorationNodeState.NOT_RESPONDING;
		}
	}

	@Override
	public boolean requestWorkableStatesFrom(String nodeAddress) {
		IProcessingClient node = nodeRefs.get(nodeAddress);
		if (node != null) {
			node.requestWorkableState(recallAddress);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean restartNode(String nodeAddress, String stateXMI) {
		IProcessingClient node = nodeRefs.get(nodeAddress);

		if (node == null) {
			log.debug("Failed to restart node at address " + nodeAddress);
			return false;
		} else {
			node.submitProblem(recallAddress, dsAddress, DesignSpaceExplorer.class.getCanonicalName(), stateXMI, nodeAddress);
			log.debug("Restart request sent to " + nodeAddress);
			return true;
		}
	}

	@Override
	public List<String> getNodeAddresses() {
		return new ArrayList<String>(nodeRefs.keySet());
	}

	@Override
	public List<String> getTransformationRuleFactories() {
		return cdse.getTransformationRuleFactories();
	}

	@Override
	public List<String> getConstraintFactories() {
		return cdse.getConstraintFactories();
	}

	@Override
	public List<String> getObjectiveFactories() {
		return cdse.getObjectiveFactories();
	}

	@Override
	public String getStateCoderFactory() {
		return cdse.getGlobalContext().getStateCoderFactory().getClass().getName();
	}

	@Override
	public IProcessingClient getNode(String nodeAddress) {
		return nodeRefs.get(nodeAddress);
	}

	@Override
	public String getRecallAddress() {
		return recallAddress;
	}

}
