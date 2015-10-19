package org.eclipse.viatra.dse.cluster.node;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.viatra.dse.api.DSETransformationRule;
import org.eclipse.viatra.dse.cluster.RemoteTransitionMetaData;
import org.eclipse.viatra.dse.cluster.interfaces.IRemoteDesignSpace;
import org.eclipse.viatra.dse.designspace.api.IState;
import org.eclipse.viatra.dse.designspace.api.ITransition;
import org.eclipse.viatra.dse.designspace.api.TransitionMetaData;

public class RemoteTransition implements ITransition {

	private static final Logger log = Logger.getLogger(RemoteTransition.class);

	static RemoteDesignSpace ds;

	static IRemoteDesignSpace remoteActor;

	static final Map<String, DSETransformationRule<?, ?>> ruleMap = new HashMap<String, DSETransformationRule<?, ?>>();

	private Object sourceStateId;
	private Object id;
	private String ruleName = null;
	private TransitionMetaData metaData = null;

	public RemoteTransition(Object sourceStateId, Object id) {
		super();
		this.sourceStateId = sourceStateId;
		this.id = id;
	}

	@Override
	public Object getId() {
		return id;
	}

	@Override
	public IState getResultsIn() {
		return ds.getLocalState(remoteActor.transition_getResultsIn(sourceStateId, id));
	}

	@Override
	public IState getFiredFrom() {
		return ds.getLocalState(sourceStateId);
	}

	@Override
	public TransitionMetaData getTransitionMetaData() {
		if (metaData == null) {
			RemoteTransitionMetaData remoteTransitionMetaData = remoteActor
					.transition_getTransitionMetaData(sourceStateId, id);
			TransitionMetaData transitionMetaData = new TransitionMetaData();
			transitionMetaData.costs = remoteTransitionMetaData.costs;
			transitionMetaData.rule = ruleMap.get(remoteTransitionMetaData.ruleName);
			metaData = transitionMetaData;
		}

		return metaData;
	}

	@Override
	public void setResultsIn(IState state) {
		remoteActor.transition_setResultsIn(sourceStateId, id, state.getId());
	}

	@Override
	public boolean isAssignedToFire() {
		return remoteActor.isAssignedToFire(sourceStateId, id);
	}

	@Override
	public boolean tryToLock() {
		if (remoteActor.tryToLock(sourceStateId, id)) {
			log.debug("Lock of transition \"" + sourceStateId + "\",\"" + id + "\" successfull");
			return true;
		} else {
			log.debug("Lock of transition \"" + sourceStateId + "\",\"" + id + "\" FAILED");
			return false;
		}

	}

}
