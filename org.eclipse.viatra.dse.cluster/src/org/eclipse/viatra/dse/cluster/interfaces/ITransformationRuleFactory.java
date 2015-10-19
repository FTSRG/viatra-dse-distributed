package org.eclipse.viatra.dse.cluster.interfaces;

import org.eclipse.incquery.runtime.exception.IncQueryException;
import org.eclipse.viatra.dse.api.DSETransformationRule;

public interface ITransformationRuleFactory {
	DSETransformationRule<?, ?> create() throws IncQueryException;
}
