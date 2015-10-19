package org.eclipse.viatra.dse.cluster.test.factories;

import org.eclipse.viatra.dse.api.Strategies;
import org.eclipse.viatra.dse.api.strategy.interfaces.IStrategy;
import org.eclipse.viatra.dse.cluster.interfaces.IStrategyFactory;

public class CreateDFS4Strategy implements IStrategyFactory {

	@Override
	public IStrategy create() {
		return Strategies.createDFSStrategy(4);
	}

}
