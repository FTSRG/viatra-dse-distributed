package org.eclipse.viatra.dse.cluster.node;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.viatra.dse.cluster.RemoteTransitionMetaData;
import org.eclipse.viatra.dse.cluster.interfaces.IRemoteDesignSpace;
import org.eclipse.viatra.dse.designspace.api.IDesignSpace;
import org.eclipse.viatra.dse.designspace.api.IDesignSpaceChangeHandler;
import org.eclipse.viatra.dse.designspace.api.IState;
import org.eclipse.viatra.dse.designspace.api.ITransition;
import org.eclipse.viatra.dse.designspace.api.TransitionMetaData;

/**
 * This is the design space implementation on the worker node. It forwards any
 * method calls on the {@link IState}, {@link ITransition} and
 * {@link IDesignSpace} interfaces to the problem host via the Remota
 * Designspace Actor.
 * 
 * @author Miki
 * 
 */
public class RemoteDesignSpace implements IDesignSpace {
	private static final Logger log = Logger.getLogger(RemoteDesignSpace.class.getName());
	static {
		log.setLevel(Level.DEBUG);
	}

	private final IRemoteDesignSpace remoteActor;

	final HashMap<Object, WeakReference<RemoteState>> stateMap = new HashMap<Object, WeakReference<RemoteState>>();

	final HashMap<IRemoteDesignSpace.TransitionId, WeakReference<RemoteTransition>> transitionMap = new HashMap<IRemoteDesignSpace.TransitionId, WeakReference<RemoteTransition>>();

	private final ReferenceQueue<RemoteState> referenceStateQueue = new ReferenceQueue<RemoteState>();
	private final ReferenceQueue<RemoteTransition> referenceTransitionQueue = new ReferenceQueue<RemoteTransition>();

	public RemoteDesignSpace(IRemoteDesignSpace remoteActor) {
		super();
		this.remoteActor = remoteActor;
		RemoteState.ds = this;
		RemoteState.remoteActor = remoteActor;
		RemoteTransition.ds = this;
		RemoteTransition.remoteActor = remoteActor;
	}

	RemoteState getLocalState(Object id) {
		WeakReference<RemoteState> weakReference = stateMap.get(id);
		RemoteState remoteState = null;

		// there has been a request before
		if (weakReference != null) {
			// use the cached object if not null
			remoteState = weakReference.get();
		}

		// if not accessed before or has been already collected
		if (remoteState == null) {

			// create the proxy object
			remoteState = new RemoteState(id);

			// create the weak ref
			weakReference = new WeakReference<RemoteState>(remoteState, referenceStateQueue);

			// save it in the cache
			stateMap.put(id, weakReference);
		}

		// set the return array
		return remoteState;
	}

	RemoteTransition getLocalTransition(Object sourceStateId, Object id) {
		WeakReference<RemoteTransition> weakReference = transitionMap.get(new IRemoteDesignSpace.TransitionId(sourceStateId, id));
		RemoteTransition remoteTransition = null;

		// there has been a request before
		if (weakReference != null) {
			// use the cached object if not null
			remoteTransition = weakReference.get();
		}

		// if not accessed before or has been already collected
		if (remoteTransition == null) {

			// create the proxy object
			remoteTransition = new RemoteTransition(sourceStateId, id);

			// create the weak ref
			weakReference = new WeakReference<RemoteTransition>(remoteTransition, referenceTransitionQueue);

			// save it in the cache
			transitionMap.put(new IRemoteDesignSpace.TransitionId(sourceStateId, id), weakReference);
		}

		// set the return array
		return remoteTransition;
	}

	@Override
	public IState[] getRoot() {
		// get remote root addresses
		Object[] objects = remoteActor.getRoot();
		// create the new local array
		RemoteState[] remotesRoots = new RemoteState[objects.length];

		// process each address and create the proxy object
		for (int i = 0; i < objects.length; i++) {

			// set the return array
			remotesRoots[i] = getLocalState(objects[i]);
		}

		// return with the local IState proxies
		return remotesRoots;
	}

	@Override
	public void addRoot(IState root) {
		remoteActor.addRoot(root.getId());
	}

	@Override
	public boolean addState(ITransition sourceTransition, Object newStateId, Map<Object, TransitionMetaData> outgoingTransitionIds) {

		Map<Object, RemoteTransitionMetaData> fakedTransitionIds = new HashMap<Object, RemoteTransitionMetaData>();

		log.debug("Adding state. Source transition: \"" + (sourceTransition == null ? "no source" : sourceTransition.getId()) + "\" state: \"" + newStateId
				+ "\"");

		for (Object key : outgoingTransitionIds.keySet()) {
			TransitionMetaData transitionMetaData = outgoingTransitionIds.get(key);
			RemoteTransitionMetaData remoteMetadata = new RemoteTransitionMetaData();
			remoteMetadata.costs = transitionMetaData.costs;
			remoteMetadata.ruleName = transitionMetaData.rule.getName();
			fakedTransitionIds.put(key, remoteMetadata);
			log.debug("Adding new transition to state: \"" + newStateId + "\" with id: \"" + key);
		}

		if (sourceTransition == null) {
			return remoteActor.addState(null, null, newStateId, fakedTransitionIds);
		} else {
			return remoteActor.addState(sourceTransition.getFiredFrom().getId(), sourceTransition.getId(), newStateId, fakedTransitionIds);
		}
	}

	@Override
	public IState getStateById(Object id) {
		long start = System.nanoTime();
		WeakReference<RemoteState> weakReference = stateMap.get(id);
		if (weakReference == null) {
			if (remoteActor.doesStateExist(id)) {
				RemoteState state = new RemoteState(id);
				weakReference = new WeakReference<RemoteState>(state);
				stateMap.put(id, weakReference);
				long duration = System.nanoTime() - start;
				log.debug("call to remote.getStateById took " + duration + " ns");
				return state;
			} else {
				return null;
			}
		} else {
			RemoteState state = weakReference.get();
			if (state == null) {
				state = new RemoteState(id);
				weakReference = new WeakReference<RemoteState>(state);
				stateMap.put(id, weakReference);
			}
			return state;
		}
	}

	@Override
	public long getNumberOfStates() {
		return remoteActor.getNumberOfStates();
	}

	@Override
	public long getNumberOfTransitions() {
		return remoteActor.getNumberOfTransitions();
	}

//	@Override
//	public long getNumberOfFiredTransitions() {
//		return remoteActor.getNumberOfFiredTransitions();
//	}

	@Override
	public void saveDesignSpace(String fileName) throws IOException {
		log.warn("Remotely saving the designspace is not allowed at this time!");
		throw new UnsupportedOperationException();
	}

	@Override
	public void addDesignSpaceChangedListener(IDesignSpaceChangeHandler changeEvent) {
		log.warn("Remote listeners are not supported at this time!");
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeDesignSpaceChangedListener(IDesignSpaceChangeHandler changeEvent) {
		log.warn("Remote listeners are not supported at this time!");
		throw new UnsupportedOperationException();
	}

//	@Override
//	public void addSolution(List<ITransition> transitions) {
//		List<IRemoteDesignSpace.TransitionId> transitionsIds = new ArrayList<IRemoteDesignSpace.TransitionId>();
//		for (ITransition t : transitions) {
//			transitionsIds.add(new TransitionId(t.getFiredFrom().getId(), t.getId()));
//		}
//		remoteActor.addSolutionTrajectory(transitionsIds);
//	}

//	@Override
//	public Map<IState, Map<IState, Collection<List<ITransition>>>> getSolutions() {
//		log.warn("Remote solution retrieval is not supported at this time!");
//		throw new UnsupportedOperationException("Remote solution retrieval is not supported at this time!");
//	}

	@Override
	public Collection<IState> getWorkableStates() {
		ArrayList<IState> workableStates = new ArrayList<IState>();
		for (Object state : remoteActor.getWorkableStates()) {
			workableStates.add(getLocalState(state));
		}

		return workableStates;
	}

	@Override
	public void attachXMIToState(IState state, String xmi) {
		remoteActor.attachXMIModelState(state.getId(), xmi);
	}
}
