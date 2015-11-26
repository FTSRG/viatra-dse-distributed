package org.eclipse.viatra.dse.cluster.interfaces;

import java.util.List;
import java.util.concurrent.TimeoutException;

import org.eclipse.viatra.dse.cluster.interfaces.IProcessingClient.ExplorationNodeState;

/**
 * This interface defines the methods of a DSE Problem host that is not visible
 * to the Nodes.
 * 
 * @author Miki
 * 
 */
public interface IProblemServerPrivate extends IProblemServer {
	void startClustering();

	ExplorationNodeState getNodeState(String address) throws TimeoutException;

	List<String> getNodeAddresses();

	String getRecallAddress();

	boolean requestWorkableStatesFrom(String nodeAddress);

	boolean restartNode(String nodeAddress, String stateXMI);

	IProcessingClient getNode(String nodeAddress);
}
