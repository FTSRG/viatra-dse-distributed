package org.eclipse.viatra.dse.cluster;

import java.util.Map;

public class NewStateChange implements IDesignSpaceChange {
	private static final long serialVersionUID = 1L;

	private final Object sourceStateId;
	private final Object transitionId;
	private final Object newStateId;
	private final Map<Object, RemoteTransitionMetaData> outgoingTransitions;
	private final String source;

	public NewStateChange(Object sourceStateId, Object transitionId, Object newStateId,
			Map<Object, RemoteTransitionMetaData> outgoingTransitions, String source) {
		super();
		this.sourceStateId = sourceStateId;
		this.transitionId = transitionId;
		this.newStateId = newStateId;
		this.outgoingTransitions = outgoingTransitions;
		this.source = source;
	}

	public Object getSourceStateId() {
		return sourceStateId;
	}

	public Object getTransitionId() {
		return transitionId;
	}

	public Object getNewStateId() {
		return newStateId;
	}

	public Map<Object, RemoteTransitionMetaData> getOutgoingTransitions() {
		return outgoingTransitions;
	}

	@Override
	public String getChangeSource() {
		return source;
	}

	@Override
	public String toString() {
		return "S: " + sourceStateId + ": " + transitionId + "->" + newStateId;
	}

	@Override
	public int hashCode() {
		return sourceStateId.hashCode() + transitionId.hashCode() + newStateId.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof NewStateChange) {
			NewStateChange other = (NewStateChange) o;
			return other.newStateId.equals(newStateId) && other.transitionId.equals(transitionId)
					&& other.newStateId.equals(newStateId);
		}
		return false;
	}
}
