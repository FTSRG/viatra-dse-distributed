package org.eclipse.viatra.dse.cluster.node;

import java.util.Collection;

import org.eclipse.viatra.dse.api.Solution;
import org.eclipse.viatra.dse.api.strategy.interfaces.ISolutionFoundHandler;
import org.eclipse.viatra.dse.base.ThreadContext;
import org.eclipse.viatra.dse.solutionstore.ISolutionStore;

public class RemoteSolutionStore implements ISolutionStore {

	@Override
	public StopExecutionType newSolution(ThreadContext context) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<Solution> getSolutions() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void registerSolutionFoundHandler(ISolutionFoundHandler handler) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isStrategyDependent() {
		return false;
	}

}
