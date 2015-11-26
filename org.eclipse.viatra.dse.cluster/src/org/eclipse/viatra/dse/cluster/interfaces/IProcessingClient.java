package org.eclipse.viatra.dse.cluster.interfaces;

import java.util.List;
import java.util.concurrent.TimeoutException;

import org.eclipse.viatra.dse.cluster.IDesignSpaceChange;

public interface IProcessingClient {
	void submitProblem(String recallAddress, String designSpaceAddress, String initiatorClass, String initalStateXMI, String identifier);
	
	void fixThreads(int threadCount);
	
	void requestWorkableState(String recallAddress);
	
	ExplorationNodeState getNodeState(String recallAddress) throws TimeoutException;
	
	void doUpdates(List<IDesignSpaceChange> changes, String recallAddress);

	enum ExplorationNodeState {
		WAITING_FOR_TASK,
		TASK_RECEIVED,
		PROCESSING,
		WAITING_FOR_NEW_ROOT,
		COMPLETED,
		NOT_RESPONDING
	}
}
