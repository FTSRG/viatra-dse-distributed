package org.eclipse.viatra.dse.cluster.test.factories;

import org.eclipse.incquery.runtime.exception.IncQueryException;
import org.eclipse.viatra.dse.api.DesignSpaceExplorer;
import org.eclipse.viatra.dse.cluster.interfaces.IObjectiveFactory;
import org.eclipse.viatra.dse.examples.bpmn.patterns.util.AbsenceOfResourceInstancesQuerySpecification;
import org.eclipse.viatra.dse.examples.bpmn.patterns.util.UnassignedTaskQuerySpecification;
import org.eclipse.viatra.dse.objectives.Comparators;
import org.eclipse.viatra.dse.objectives.IObjective;
import org.eclipse.viatra.dse.objectives.impl.WeightedQueriesSoftObjective;

public class CreateGuidanceObjectiveFactory implements IObjectiveFactory {

	@Override
	public IObjective create(DesignSpaceExplorer dse) throws IncQueryException {
		return new WeightedQueriesSoftObjective()
				.withConstraint(AbsenceOfResourceInstancesQuerySpecification.instance(), 1)
				.withConstraint(UnassignedTaskQuerySpecification.instance(), 10)
				.withComparator(Comparators.LOWER_IS_BETTER).withLevel(0);
	}

}
