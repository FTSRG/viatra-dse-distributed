package org.eclipse.viatra.dse.cluster.interfaces;

import org.eclipse.viatra.dse.api.IDesignSpaceExplorer;
import org.eclipse.viatra.dse.statecode.IStateCoderFactory;

public interface IRemoteStateCoderFactory {
	IStateCoderFactory create(IDesignSpaceExplorer dse);
}
