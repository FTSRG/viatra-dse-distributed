package org.eclipse.viatra.dse.cluster.host;

import org.eclipse.viatra.dse.api.IDesignSpaceExplorer;
import org.eclipse.viatra.dse.cluster.interfaces.IRemoteStateCoderFactory;
import org.eclipse.viatra.dse.statecode.IStateCoderFactory;
import org.eclipse.viatra.dse.statecode.incrementalgraph.IncrementalGraphHasherFactory;

public class RemoteIncrementalStateCoderFactory implements IRemoteStateCoderFactory {

	@Override
	public IStateCoderFactory create(IDesignSpaceExplorer dse) {
		return new IncrementalGraphHasherFactory(dse.getMetaModelPackages());
	}

}
