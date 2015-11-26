package org.eclipse.viatra.dse.cluster.interfaces;

import org.eclipse.viatra.dse.api.DesignSpaceExplorer;
import org.eclipse.viatra.dse.statecode.IStateCoderFactory;

public interface IRemoteStateCoderFactory {
	IStateCoderFactory create(DesignSpaceExplorer dse);
}
