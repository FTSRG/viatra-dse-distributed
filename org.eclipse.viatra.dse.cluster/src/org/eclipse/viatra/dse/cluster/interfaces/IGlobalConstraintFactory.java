package org.eclipse.viatra.dse.cluster.interfaces;

import org.eclipse.incquery.runtime.exception.IncQueryException;
import org.eclipse.viatra.dse.objectives.IGlobalConstraint;

public interface IGlobalConstraintFactory {
	IGlobalConstraint create() throws IncQueryException;
}
