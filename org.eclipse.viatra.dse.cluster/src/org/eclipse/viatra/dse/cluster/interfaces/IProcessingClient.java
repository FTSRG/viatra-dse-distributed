package org.eclipse.viatra.dse.cluster.interfaces;

import java.util.concurrent.TimeoutException;



public interface IProcessingClient {
	void submitProblem(String recallAddress, String designSpaceAddress, String initiatorClass, String initalStateXMI);
	
	void fixThreads(int threadCount);
	
	void requestWorkableState(String recallAddress);
	
	ExplorationNodeState getNodeState(String recallAddress) throws TimeoutException;
	
	enum ExplorationNodeState {
		WAITING_FOR_TASK,
		TASK_RECEIVED,
		PROCESSING,
		WAITING_FOR_NEW_ROOT,
		COMPLETED,
		NOT_RESPONDING
	}
}
