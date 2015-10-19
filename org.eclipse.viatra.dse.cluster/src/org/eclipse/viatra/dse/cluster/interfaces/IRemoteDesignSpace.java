package org.eclipse.viatra.dse.cluster.interfaces;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.viatra.dse.designspace.api.IState.TraversalStateType;
import org.eclipse.viatra.dse.cluster.RemoteTransitionMetaData;
import org.eclipse.viatra.dse.designspace.api.TransitionMetaData;

public interface IRemoteDesignSpace {
	public final class TransitionId {
		public final Object sourceStateId;
		public final Object transitionId;
	
		public TransitionId(Object sourceStateId, Object transitionId) {
			super();
			this.sourceStateId = sourceStateId;
			this.transitionId = transitionId;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof TransitionId))
				return false;
			TransitionId key = (TransitionId) o;
			return sourceStateId == key.sourceStateId && transitionId == key.transitionId;
		}
	
		@Override
		public int hashCode() {
			return sourceStateId.hashCode() + transitionId.hashCode();
		}
	}

	Object[] getRoot();

	boolean addState(Object transitionSourceState, Object transition, Object newState, Map<Object, RemoteTransitionMetaData> outgoingTransitions);

	boolean doesStateExist(Object id);

	long getNumberOfStates();

	long getNumberOfTransitions();

	long getNumberOfFiredTransitions();

	void addRoot(Object id);

	TraversalStateType state_getTraversalState(Object id);

	void state_setTraversalState(Object id, TraversalStateType traversalState);

	Collection<Object> state_getIncomingTransitions(Object id);
	
	Collection<Object> state_getOutgoingTransitions(Object id);

	boolean state_isProcessed(Object id);

	void state_setProcessed(Object id);

	Object transition_getResultsIn(Object sourceId, Object id);

	RemoteTransitionMetaData transition_getTransitionMetaData(Object sourceId, Object id);

	void transition_setResultsIn(Object sourceStateId, Object transitionId, Object newStateId);

	boolean isAssignedToFire(Object sourceStateId, Object id);

	boolean tryToLock(Object sourceStateId, Object id);

//	void addSolutionTrajectory(List<TransitionId> transitions);

	void attachXMIModelState(Object stateId, String xmiString);
	
	String getXMIModelState(Object stateId);
	
	List<String> getWorkableStates();
}
