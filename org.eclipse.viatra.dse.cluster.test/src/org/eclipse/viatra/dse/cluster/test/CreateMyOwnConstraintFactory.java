package org.eclipse.viatra.dse.cluster.test;

import org.eclipse.incquery.runtime.exception.IncQueryException;
import org.eclipse.viatra.dse.cluster.interfaces.IGlobalConstraintFactory;
import org.eclipse.viatra.dse.examples.bpmn.objectives.MyOwnConstraint;
import org.eclipse.viatra.dse.objectives.IGlobalConstraint;

public class CreateMyOwnConstraintFactory implements IGlobalConstraintFactory {

	@Override
	public IGlobalConstraint create() throws IncQueryException {
		return new MyOwnConstraint();
	}

}
