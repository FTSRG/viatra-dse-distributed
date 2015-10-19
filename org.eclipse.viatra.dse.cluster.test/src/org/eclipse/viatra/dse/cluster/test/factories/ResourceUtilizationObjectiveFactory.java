package org.eclipse.viatra.dse.cluster.test.factories;

import org.eclipse.incquery.runtime.exception.IncQueryException;
import org.eclipse.viatra.dse.api.DesignSpaceExplorer;
import org.eclipse.viatra.dse.cluster.interfaces.IObjectiveFactory;
import org.eclipse.viatra.dse.examples.bpmn.objectives.MinResourceUsageSoftObjective;
import org.eclipse.viatra.dse.objectives.Comparators;
import org.eclipse.viatra.dse.objectives.IObjective;

public class ResourceUtilizationObjectiveFactory implements IObjectiveFactory {

	@Override
	public IObjective create(DesignSpaceExplorer dse) throws IncQueryException {
		return new MinResourceUsageSoftObjective().withComparator(Comparators.HIGHER_IS_BETTER).withLevel(1);
	}

}
