package org.eclipse.viatra.dse.cluster.interfaces;

import java.util.List;
import java.util.Set;

/**
 * This interface defines the requests that a DSE worker node can send to the
 * problem host.
 * 
 * @author Miki
 * 
 */
public interface IProblemServer {
	final String ACTOR_SYSTEM_NAME = "DSEActorSystem";

	final String REMOTE_CLASSLOADER_HOST_NAME = "ActorHost";
	
	final String REMOTE_DESIGNSPACE_HOST_NAME = "RemoteDesignspaceHost";

	final String REMOTE_NODE_NAME = "ActorNode";

	String getCoreClassname();

	/**
	 * Request the code for a given class to be loaded.
	 * 
	 * @param className
	 * @return
	 * @throws Exception
	 */
	byte[] loadClass(String className) throws Exception;

	String ping();

	Set<String> getRemoteMetamodelPackages();

//	/**
//	 * This Map defines the configuration mapping. The Key of the map is the
//	 * name of the setter method to be used on  to be called on the initiator class, whereas the List<String> is
//	 * the list of method names whose returns should be passed as parameters.
//	 * 
//	 * @return
//	 */
//	Map<String, List<String>> getStrategyConfigurationMap();

	String getStrategyName();
	
	String getModelXMI();

	List<String> getTransformationRuleFactories();
	List<String> getConstraintFactories();
	List<String> getObjectiveFactories();
	
	String getStateCoderFactory();
}
