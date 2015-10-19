package org.eclipse.viatra.dse.cluster.test.factories;

import org.eclipse.incquery.runtime.exception.IncQueryException;
import org.eclipse.viatra.dse.cluster.interfaces.IGlobalConstraintFactory;
import org.eclipse.viatra.dse.examples.bpmn.patterns.util.UnrequiredResourceInstanceQuerySpecification;
import org.eclipse.viatra.dse.objectives.IGlobalConstraint;
import org.eclipse.viatra.dse.objectives.impl.ModelQueriesGlobalConstraint;

public class CreateGlobalConstraintFactory implements IGlobalConstraintFactory {

	@Override
	public IGlobalConstraint create() throws IncQueryException {
		return new ModelQueriesGlobalConstraint()
                .withConstraint(UnrequiredResourceInstanceQuerySpecification.instance());
	}

}
