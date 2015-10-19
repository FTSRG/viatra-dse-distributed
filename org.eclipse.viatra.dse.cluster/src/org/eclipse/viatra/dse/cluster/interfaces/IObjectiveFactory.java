package org.eclipse.viatra.dse.cluster.interfaces;

import org.eclipse.incquery.runtime.exception.IncQueryException;
import org.eclipse.viatra.dse.api.DesignSpaceExplorer;
import org.eclipse.viatra.dse.objectives.IObjective;

public interface IObjectiveFactory {
	IObjective create(DesignSpaceExplorer dse) throws IncQueryException;
}