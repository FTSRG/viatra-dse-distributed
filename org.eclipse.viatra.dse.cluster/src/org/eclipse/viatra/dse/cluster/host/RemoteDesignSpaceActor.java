package org.eclipse.viatra.dse.cluster.host;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.viatra.dse.cluster.RemoteTransitionMetaData;
import org.eclipse.viatra.dse.cluster.interfaces.IRemoteDesignSpace;
import org.eclipse.viatra.dse.designspace.api.IDesignSpace;
import org.eclipse.viatra.dse.designspace.api.IState;
import org.eclipse.viatra.dse.designspace.api.IState.TraversalStateType;
import org.eclipse.viatra.dse.designspace.api.ITransition;
import org.eclipse.viatra.dse.designspace.api.TransitionMetaData;

import akka.actor.ActorSystem;

public class RemoteDesignSpaceActor implements IRemoteDesignSpace {

	private static final Logger log = Logger.getLogger(RemoteDesignSpaceActor.class.getName());

	// private IDesignSpace localDesignSpace;

	private final ClusteredDesignSpaceExplorer cdse;

	private final ActorSystem system;

	public RemoteDesignSpaceActor(ClusteredDesignSpaceExplorer cdse, ActorSystem system) {
		super();
		this.cdse = cdse;
		this.system = system;
		// this.localDesignSpace = cdse.getGlobalContext().getDesignSpace();
	}

	private IDesignSpace localDesignSpace() {
		return cdse.getGlobalContext().getDesignSpace();
	}

	@Override
	public Object[] getRoot() {
		IState[] roots = localDesignSpace().getRoot();
		Object[] objects = new Object[roots.length];
		for (int i = 0; i < roots.length; i++) {
			objects[i] = roots[i].getId();
		}
		return objects;
	}

	private Map<Object, TransitionMetaData> convertRemoteToLocalMetadata(Map<Object, RemoteTransitionMetaData> map) {
		Map<Object, TransitionMetaData> localMetadata = new HashMap<Object, TransitionMetaData>();
		for (Object key : map.keySet()) {
			TransitionMetaData transitionMetaData = new TransitionMetaData();
			transitionMetaData.costs = map.get(key).costs;
			transitionMetaData.rule = cdse.convertToLocalRule(map.get(key).ruleName);
			localMetadata.put(key, transitionMetaData);
		}
		return localMetadata;
	}

	@Override
	public boolean addState(Object transitionSourceState, Object transition, Object newState,
			Map<Object, RemoteTransitionMetaData> outgoingTransitions) {
		if (transitionSourceState == null && transition == null) {
			return localDesignSpace().addState(null, newState, convertRemoteToLocalMetadata(outgoingTransitions));
		} else {
			IState sourceState = localDesignSpace().getStateById(transitionSourceState);
			for (ITransition t : sourceState.getOutgoingTransitions()) {
				if (t.getId().equals(transition)) {
					return localDesignSpace().addState(t, newState, convertRemoteToLocalMetadata(outgoingTransitions));
				}
			}
			log.error(
					"Remotely received state does not fit into the Designspace. The address received as the source is not contained in the design space.");
			throw new Error("Inconsistent Designspace");
		}
	}

	@Override
	public boolean doesStateExist(Object id) {
		return localDesignSpace().getStateById(id) != null;
	}

	@Override
	public long getNumberOfStates() {
		return localDesignSpace().getNumberOfStates();
	}

	@Override
	public long getNumberOfTransitions() {
		return localDesignSpace().getNumberOfTransitions();
	}

	@Override
	public void addRoot(Object id) {
		localDesignSpace().addRoot(localDesignSpace().getStateById(id));
	}

	@Override
	public TraversalStateType state_getTraversalState(Object id) {
		return localDesignSpace().getStateById(id).getTraversalState();
	}

	@Override
	public void state_setTraversalState(Object id, TraversalStateType traversalState) {
		localDesignSpace().getStateById(id).setTraversalState(traversalState);
	}

	@Override
	public Collection<Object> state_getIncomingTransitions(Object id) {
		Collection<? extends ITransition> incomingTransitions = localDesignSpace().getStateById(id)
				.getIncomingTransitions();
		Collection<Object> transitions = new ArrayList<Object>();
		for (ITransition transition : incomingTransitions) {
			transitions.add(transition.getId());
		}
		return transitions;
	}

	@Override
	public Collection<Object> state_getOutgoingTransitions(Object id) {
		Collection<? extends ITransition> outgoingTransitions = localDesignSpace().getStateById(id)
				.getOutgoingTransitions();
		Collection<Object> transitions = new ArrayList<Object>();
		for (ITransition transition : outgoingTransitions) {
			transitions.add(transition.getId());
		}
		return transitions;
	}

	@Override
	public boolean state_isProcessed(Object id) {
		return localDesignSpace().getStateById(id).isProcessed();
	}

	@Override
	public void state_setProcessed(Object id) {
		localDesignSpace().getStateById(id).setProcessed();
	}

	private ITransition getTransition(Object sourceId, Object id) {
		IState sourceState = localDesignSpace().getStateById(sourceId);
		for (ITransition t : sourceState.getOutgoingTransitions()) {
			if (t.getId().equals(id)) {
				return t;
			}
		}

		return null;
	}

	@Override
	public Object transition_getResultsIn(Object sourceId, Object id) {
		IState resultsIn = getTransition(sourceId, id).getResultsIn();
		if (resultsIn == null) {
			return null;
		} else {
			return resultsIn.getId();
		}
	}

	@Override
	public void transition_setResultsIn(Object sourceStateId, Object transitionId, Object newStateId) {
		getTransition(sourceStateId, transitionId).setResultsIn(localDesignSpace().getStateById(newStateId));
	}

	@Override
	public RemoteTransitionMetaData transition_getTransitionMetaData(Object sourceStateId, Object transitionId) {
		TransitionMetaData transitionMetaData = getTransition(sourceStateId, transitionId).getTransitionMetaData();
		RemoteTransitionMetaData remoteTransitionMetaData = new RemoteTransitionMetaData();
		remoteTransitionMetaData.costs = transitionMetaData.costs;
		remoteTransitionMetaData.ruleName = transitionMetaData.rule.getName();
		return remoteTransitionMetaData;
	}

	@Override
	public boolean isAssignedToFire(Object sourceStateId, Object id) {
		return getTransition(sourceStateId, id).isAssignedToFire();
	}

	@Override
	public boolean tryToLock(Object sourceStateId, Object id) {
		return getTransition(sourceStateId, id).tryToLock();
	}

	@Override
	public void attachXMIModelState(Object stateId, String xmiString) {
		IState state = localDesignSpace().getStateById(stateId);
		localDesignSpace().attachXMIToState(state, xmiString);
		log.info("XMI string attached to state " + stateId);
	}

	@Override
	public String getXMIModelState(Object stateId) {
		return localDesignSpace().getStateById(stateId).getXMIModelState();
	}

	@Override
	public List<String> getWorkableStates() {
		List<String> states = new ArrayList<String>();
		for (IState stateId : localDesignSpace().getWorkableStates()) {
			states.add(stateId.getXMIModelState());
		}

		return states;
	}

	@Override
	public long getNumberOfFiredTransitions() {
		return 0;
	}
}