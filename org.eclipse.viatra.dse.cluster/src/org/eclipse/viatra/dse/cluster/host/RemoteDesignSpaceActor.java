package org.eclipse.viatra.dse.cluster.host;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.viatra.dse.api.DSETransformationRule;
import org.eclipse.viatra.dse.cluster.IDesignSpaceChange;
import org.eclipse.viatra.dse.cluster.NewRootChange;
import org.eclipse.viatra.dse.cluster.NewStateChange;
import org.eclipse.viatra.dse.cluster.RemoteTransitionMetaData;
import org.eclipse.viatra.dse.cluster.interfaces.IProcessingClient;
import org.eclipse.viatra.dse.cluster.interfaces.IRemoteDesignSpace;
import org.eclipse.viatra.dse.designspace.api.IDesignSpace;
import org.eclipse.viatra.dse.designspace.api.IState;
import org.eclipse.viatra.dse.designspace.api.IState.TraversalStateType;
import org.eclipse.viatra.dse.designspace.api.ITransition;
import org.eclipse.viatra.dse.designspace.api.TransitionMetaData;

import akka.actor.ActorSystem;
import akka.dispatch.Futures;
import scala.concurrent.Future;

public class RemoteDesignSpaceActor implements IRemoteDesignSpace {

	private static final Logger log = Logger.getLogger(RemoteDesignSpaceActor.class.getName());

	// private IDesignSpace localDesignSpace;

	private final ClusteredDesignSpaceExplorer cdse;

	private final ActorSystem system;

	public RemoteDesignSpaceActor(ClusteredDesignSpaceExplorer cdse, ActorSystem system) {
		super();
		this.cdse = cdse;
		this.system = system;
		for (String node : cdse.getNodes()) {
			changeSet.put(node, new HashSet<IDesignSpaceChange>());
		}
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
		throw new UnsupportedOperationException();
		// IState state = localDesignSpace().getStateById(stateId);
		// localDesignSpace().attachXMIToState(state, xmiString);
		// log.info("XMI string attached to state " + stateId);
	}

	@Override
	public String getXMIModelState(Object stateId) {
		throw new UnsupportedOperationException();
		// return localDesignSpace().getStateById(stateId).getXMIModelState();
	}

	@Override
	public List<String> getWorkableStates() {
		throw new UnsupportedOperationException();
		// List<String> states = new ArrayList<String>();
		// for (IState stateId : localDesignSpace().getWorkableStates()) {
		// states.add(stateId.getXMIModelState());
		// }
		//
		// return states;
	}

	@Override
	public long getNumberOfFiredTransitions() {
		return 0;
	}

	@Override
	public Future<Boolean> addStateAsync(Object transitionSourceState, Object transition, Object newState,
			Map<Object, RemoteTransitionMetaData> outgoingTransitions) {
		return Futures.successful(addState(transitionSourceState, transition, newState, outgoingTransitions));
	}

	public Map<String, Integer> incomingChanges = new HashMap<String, Integer>();

	private List<IDesignSpaceChange> outOfOrderChanges = new ArrayList<IDesignSpaceChange>();

	private void merge(IDesignSpaceChange change) {
		if (change instanceof NewRootChange) {
			NewRootChange newRootChange = (NewRootChange) change;

			localDesignSpace().addState(null, newRootChange.getRootId(),
					convertLocal(newRootChange.getOutgoingTransitions()), false);
			System.out.println("DS root is now: " + localDesignSpace().getRoot());
		}
		if (change instanceof NewStateChange) {
			NewStateChange newStateChange = (NewStateChange) change;
			IState sourceState = localDesignSpace().getStateById(newStateChange.getSourceStateId());
			if (sourceState != null) {

				ITransition sourceTransition = null;
				for (ITransition t : sourceState.getOutgoingTransitions()) {
					if (t.getId().equals(newStateChange.getTransitionId())) {
						sourceTransition = t;
					}
				}
				if (sourceTransition == null) {
					throw new Error();
				}

				localDesignSpace().addState(sourceTransition, newStateChange.getNewStateId(),
						convertLocal(newStateChange.getOutgoingTransitions()), false);
			} else {
				outOfOrderChanges.add(change);
			}
		}
	}

	@Override
	public void doUpdates(List<IDesignSpaceChange> remoteChanges, String originator) {
		enqueueThirdPartyChanges(remoteChanges, originator);

		Integer integer = incomingChanges.get(originator);
		if (integer == null) {
			incomingChanges.put(originator, remoteChanges.size());
		} else {
			incomingChanges.put(originator, integer + remoteChanges.size());
		}
		boolean insertLocally = true;
		if (insertLocally) {
			for (IDesignSpaceChange change : remoteChanges) {
				merge(change);
			}

			while (!outOfOrderChanges.isEmpty()) {
				List<IDesignSpaceChange> remainingOutOfOrderChanges = outOfOrderChanges;
				outOfOrderChanges = new ArrayList<IDesignSpaceChange>();

				for (IDesignSpaceChange change : remainingOutOfOrderChanges) {
					merge(change);
				}
			}
		}

		List<IDesignSpaceChange> changeList = new ArrayList<IDesignSpaceChange>();
		synchronized (changeSet) {
			changeList.addAll(changeSet.get(originator));
			changeSet.get(originator).clear();
		}

		if (!changeList.isEmpty()) {
			IProcessingClient node = cdse.problemHostActor.getNode(originator);
			System.out.println("pushing " + changeList.size() + " changes to " + originator);

			node.doUpdates(changeList, cdse.problemHostActor.getRecallAddress());
		}
		// System.out.println("sending "+hashSet.size()+" changes to
		// "+originator);

		// System.out.println("Contributors:\n");
		// for (String id : incomingChanges.keySet()) {
		// System.out.println("id: " + id + ":\t" + incomingChanges.get(id));
		// }

		// return changeList;
	}

	public void enqueueThirdPartyChanges(Collection<IDesignSpaceChange> outgoing, String originator) {
		synchronized (changeSet) {
			for (String key : changeSet.keySet()) {
				if (!key.equals(originator)) {
					changeSet.get(key).addAll(outgoing);
				}
			}
		}
	}

	private Map<Object, TransitionMetaData> convertLocal(Map<Object, RemoteTransitionMetaData> remoteMeta) {
		Map<Object, TransitionMetaData> metadataMap = new HashMap<Object, TransitionMetaData>();
		for (Object key : remoteMeta.keySet()) {
			RemoteTransitionMetaData remoteTransitionMetaData = remoteMeta.get(key);
			DSETransformationRule<?, ?> localRule = cdse.convertToLocalRule(remoteTransitionMetaData.ruleName);

			TransitionMetaData transitionMetaData = new TransitionMetaData();
			transitionMetaData.costs = remoteTransitionMetaData.costs;
			transitionMetaData.rule = localRule;
			metadataMap.put(key, transitionMetaData);
		}
		return metadataMap;
	}

	private final Map<String, Collection<IDesignSpaceChange>> changeSet = new HashMap<String, Collection<IDesignSpaceChange>>();
}