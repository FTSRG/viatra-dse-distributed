package org.eclipse.viatra.dse.cluster.test.factories;

import org.eclipse.incquery.runtime.exception.IncQueryException;
import org.eclipse.viatra.dse.api.DSETransformationRule;
import org.eclipse.viatra.dse.api.DesignSpaceExplorer;
import org.eclipse.viatra.dse.cluster.interfaces.IObjectiveFactory;
import org.eclipse.viatra.dse.examples.bpmn.dse.BpmnExamples.CostOfCreateResource;
import org.eclipse.viatra.dse.examples.bpmn.patterns.util.CreateResourceQuerySpecification;
import org.eclipse.viatra.dse.examples.bpmn.patterns.util.MakeParallelQuerySpecification;
import org.eclipse.viatra.dse.examples.bpmn.patterns.util.MakeSequentialQuerySpecification;
import org.eclipse.viatra.dse.objectives.Comparators;
import org.eclipse.viatra.dse.objectives.IObjective;
import org.eclipse.viatra.dse.objectives.impl.TrajectoryCostSoftObjective;

public class CreateCostsObjectiveFactory implements IObjectiveFactory {

	@Override
	public IObjective create(DesignSpaceExplorer dse) throws IncQueryException {
		DSETransformationRule<?, ?> createResourceRule = null;
		DSETransformationRule<?, ?> makeSequentialRule = null;
		DSETransformationRule<?, ?> makeParallelRule = null;
		for (DSETransformationRule<?, ?> rule : dse.getGlobalContext().getTransformations()) {
			if (rule.getName().equals(CreateResourceQuerySpecification.instance().getFullyQualifiedName())) {
				createResourceRule = rule;
			}
			if (rule.getName().equals(MakeParallelQuerySpecification.instance().getFullyQualifiedName())) {
				makeParallelRule = rule;
			}
			if (rule.getName().equals(MakeSequentialQuerySpecification.instance().getFullyQualifiedName())) {
				makeSequentialRule = rule;
			}
		}		
		
		return new TrajectoryCostSoftObjective()
                .withActivationCost(createResourceRule, new CostOfCreateResource())
                .withRuleCost(makeSequentialRule, 1)
                .withRuleCost(makeParallelRule, 1)
                .withComparator(Comparators.LOWER_IS_BETTER)
                .withLevel(1);
	}

}
