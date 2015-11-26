package org.eclipse.viatra.dse.cluster;

import java.io.Serializable;

public interface IDesignSpaceChange extends Serializable {
	String getChangeSource();
}
