package org.eclipse.viatra.dse.cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.viatra.dse.api.DSETransformationRule;
import org.eclipse.viatra.dse.cluster.interfaces.IRemoteDesignSpace;
import org.eclipse.viatra.dse.designspace.api.IDesignSpace;
import org.eclipse.viatra.dse.designspace.api.IDesignSpaceChangeHandler;
import org.eclipse.viatra.dse.designspace.api.IState;
import org.eclipse.viatra.dse.designspace.api.ITransition;
import org.eclipse.viatra.dse.designspace.api.TransitionMetaData;

public class RemoteDesignSpaceSynchronizer implements IDesignSpaceChangeHandler {

	private IRemoteDesignSpace remote;
	private IDesignSpace local;

	public final Map<String, DSETransformationRule<?, ?>> ruleMeta;

	public RemoteDesignSpaceSynchronizer(IDesignSpace local, IRemoteDesignSpace remote, String identifier) {
		this.remote = remote;
		this.local = local;
		ruleMeta = new HashMap<String, DSETransformationRule<?, ?>>();
		this.identifier = identifier;
	}

	private final HashSet<IDesignSpaceChange> hashSet = new HashSet<IDesignSpaceChange>();

	/**
	 * Handles the remote dispatch of a local event so that runners on other
	 * nodes can eventually receive the data.
	 */
	@Override
	public void newStateAdded(ITransition sourceTransition, IState newStateId) {
		if (sourceTransition == null) {
			return;
		}

		NewStateChange change = new NewStateChange(sourceTransition.getFiredFrom().getId(), sourceTransition.getId(),
				newStateId.getId(), convert(newStateId.getOutgoingTransitions()), "me");

		if (appliedRemoteChanges.contains(change)) {
			appliedRemoteChanges.remove(change);
			return;
		}

		List<IDesignSpaceChange> push = null;
		synchronized (hashSet) {
			hashSet.add(change);
			if (hashSet.size() > 0) {
				push = new ArrayList<IDesignSpaceChange>();
				push.addAll(hashSet);
				hashSet.clear();
			}
		}

		if (push != null) {
			remote.doUpdates(push, identifier);
//			process(updates);
		}
	}

	public void processIncomingChanges(List<IDesignSpaceChange> updates) {
		List<IDesignSpaceChange> failures = new ArrayList<IDesignSpaceChange>();
		for (IDesignSpaceChange remoteChange : updates) {
			// if (remoteChange instanceof NewStateChange) {
			// NewStateChange newStateChange = (NewStateChange) remoteChange;

			if (!merge(remoteChange)) {
				failures.add(remoteChange);
			}
			// local.addState(), newStateId, outgoingTransitionIds)
			// }
		}
		while (!failures.isEmpty()) {
			List<IDesignSpaceChange> previousFailures = failures;
			failures = new ArrayList<IDesignSpaceChange>();
			for (IDesignSpaceChange change : previousFailures) {
				if (!merge(change)) {
					failures.add(change);
				}
			}
		}
	}

	private final String identifier;

	/**
	 * This method incorporates remotely computed changes to the design space so
	 * that the local processes can take advantage of this knowledge.
	 * 
	 * @param change
	 */
	public void mergeRemoteEvent(IDesignSpaceChange change) {
		System.out.println("Merging event " + change);
	}

	@Override
	public void newTransitionAdded(ITransition transition) {

	}

	@Override
	public void transitionFired(ITransition transition) {
		// System.out.println("New fired transition: " +
		// transition.getFiredFrom().getId() + ":" + transition.getId()
		// + "->" + transition.getResultsIn().getId());
	}

	@Override
	public void newRootAdded(IState state) {
		NewRootChange newRootChange = new NewRootChange(state.getId(), convert(state.getOutgoingTransitions()), "me");

		if (appliedRemoteChanges.contains(newRootChange)) {
			appliedRemoteChanges.remove(newRootChange);
			return;
		}

		List<IDesignSpaceChange> changeList = null;
		synchronized (hashSet) {
			hashSet.add(newRootChange);
			changeList = new ArrayList<IDesignSpaceChange>();
			changeList.addAll(hashSet);
			hashSet.clear();
		}

		if (changeList != null) {
			remote.doUpdates(changeList, identifier);
//			if (updates != null) {
//				process(updates);
//			}
		}

		System.out.println("Root added " + state.getId());
	}
	
	

	private TransitionMetaData convertLocal(RemoteTransitionMetaData remote) {
		TransitionMetaData meta = new TransitionMetaData();
		meta.rule = ruleMeta.get(remote.ruleName);
		meta.costs = remote.costs;
		return meta;
	}

	private RemoteTransitionMetaData convertRemote(TransitionMetaData local) {
		RemoteTransitionMetaData remoteTransitionMetaData = new RemoteTransitionMetaData();
		remoteTransitionMetaData.costs = local.costs;
		remoteTransitionMetaData.ruleName = local.rule.getName();
		return remoteTransitionMetaData;
	}

	private Map<Object, RemoteTransitionMetaData> convert(Collection<? extends ITransition> outgoing) {
		Map<Object, RemoteTransitionMetaData> trans = new HashMap<Object, RemoteTransitionMetaData>();
		for (ITransition transition : outgoing) {
			trans.put(transition.getId(), convertRemote(transition.getTransitionMetaData()));
		}
		return trans;
	}

	private final Set<IDesignSpaceChange> appliedRemoteChanges = new HashSet<IDesignSpaceChange>();

	private boolean merge(IDesignSpaceChange change) {
		if (change instanceof NewRootChange) {
			NewRootChange newRootChange = (NewRootChange) change;

			local.addState(null, newRootChange.getRootId(), convertLocal(newRootChange.getOutgoingTransitions()),
					false);
			System.out.println("DS root is now: " + local.getRoot());
			return true;
		}
		if (change instanceof NewStateChange) {
			NewStateChange newStateChange = (NewStateChange) change;
			IState sourceState = local.getStateById(newStateChange.getSourceStateId());
			if (sourceState != null) {
//				System.out.println("source state is id: " + newStateChange.getSourceStateId() + " locally found id: "
//						+ sourceState);
				ITransition sourceTransition = null;
				for (ITransition t : sourceState.getOutgoingTransitions()) {
					if (t.getId().equals(newStateChange.getTransitionId())) {
						sourceTransition = t;
					}
				}
				if (sourceTransition == null) {
					throw new Error();
				}

				appliedRemoteChanges.add(change);
				local.addState(sourceTransition, newStateChange.getNewStateId(),
						convertLocal(newStateChange.getOutgoingTransitions()), false);
				return true;
			} else {
				return false;						
			}
		}
		throw new Error();
	}

	private Map<Object, TransitionMetaData> convertLocal(Map<Object, RemoteTransitionMetaData> remoteMeta) {
		Map<Object, TransitionMetaData> metadataMap = new HashMap<Object, TransitionMetaData>();
		for (Object key : remoteMeta.keySet()) {
			RemoteTransitionMetaData remoteTransitionMetaData = remoteMeta.get(key);
			TransitionMetaData transitionMetaData = convertLocal(remoteTransitionMetaData);
			metadataMap.put(key, transitionMetaData);
		}
		return metadataMap;
	}
}
