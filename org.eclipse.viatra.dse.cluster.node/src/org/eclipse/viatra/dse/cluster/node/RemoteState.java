package org.eclipse.viatra.dse.cluster.node;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.eclipse.viatra.dse.cluster.interfaces.IRemoteDesignSpace;
import org.eclipse.viatra.dse.designspace.api.IState;
import org.eclipse.viatra.dse.designspace.api.ITransition;

public class RemoteState implements IState {

	private static final Logger log = Logger.getLogger(RemoteState.class.getName());

	static RemoteDesignSpace ds;
	static IRemoteDesignSpace remoteActor;

	private final Object address;
	private boolean processed = false;
	Collection<ITransition> outgoingTransitions = null;

	public RemoteState(Object address) {
		super();
		this.address = address;
	}

	@Override
	public TraversalStateType getTraversalState() {
		return remoteActor.state_getTraversalState(address);
	}

	@Override
	public void setTraversalState(TraversalStateType traversalState) {
		remoteActor.state_setTraversalState(address, traversalState);
	}

	@Override
	public Object getId() {
		return address;
	}

	@Override
	public Collection<? extends ITransition> getIncomingTransitions() {
		Collection<ITransition> transitions = new ArrayList<ITransition>();
		log.info("remoteActor.state_getIncomingTransitions(" + address + ")");
		for (Object key : remoteActor.state_getIncomingTransitions(address)) {
			transitions.add(ds.getLocalTransition(address, key));
		}
		return transitions;
	}

	/**
	 * Returns the outgoing transitions. If the node is processed, then it is
	 * cached for future calls.
	 */
	@Override
	public Collection<? extends ITransition> getOutgoingTransitions() {

		// check if it is cached
		if (outgoingTransitions == null) {
			// retrieve remote data
			Collection<ITransition> transitions = retrieveOutgoingTransitions();

			// if it is final
			if (isProcessed()) {
				// cache it
				outgoingTransitions = transitions;
			}

			// return with the list in any case
			return transitions;
		} else {
			// return cache
			return outgoingTransitions;
		}
	}

	@Override
	public boolean isProcessed() {
		if (processed) {
			return true;
		} else {
			processed = remoteActor.state_isProcessed(address);
			return processed;
		}
	}

	@Override
	public void setProcessed() {
		remoteActor.state_setProcessed(address);
		processed = true;
	}

	@Override
	public void attachXMIModelState(String modelString) {
		throw new UnsupportedOperationException("Do this through the IDesignSpaceInterface!");
	}

	@Override
	public String getXMIModelState() {
		return remoteActor.getXMIModelState(address);
	}

	private Collection<ITransition> retrieveOutgoingTransitions() {
		Collection<ITransition> transitions = new ArrayList<ITransition>();
		log.debug("call to remoteActor.state_getOutgoingTransitions(" + address + ")");
		for (Object key : remoteActor.state_getOutgoingTransitions(address)) {
			transitions.add(ds.getLocalTransition(address, key));
		}
		return transitions;
	}
}
