package org.eclipse.viatra.dse.cluster;

import java.util.Map;

public class NewRootChange implements IDesignSpaceChange {
	private static final long serialVersionUID = 1L;

	private Object rootId;

	private final Map<Object, RemoteTransitionMetaData> outgoingTransitions;
	private final String source;

	public NewRootChange(Object rootId, Map<Object, RemoteTransitionMetaData> outgoingTransitions, String source) {
		super();
		this.rootId = rootId;
		this.outgoingTransitions = outgoingTransitions;
		this.source = source;
	}

	public Object getRootId() {
		return rootId;
	}

	public void setRootId(Object rootId) {
		this.rootId = rootId;
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
		return "Root: " + rootId;
	}
	@Override
	public int hashCode() {
		return rootId.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof NewRootChange) {
			NewRootChange other = (NewRootChange) o;
			return other.rootId.equals(rootId);
		}
		return false;
	}
}
