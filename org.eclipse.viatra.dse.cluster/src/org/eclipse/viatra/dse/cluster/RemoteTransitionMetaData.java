package org.eclipse.viatra.dse.cluster;

import java.io.Serializable;
import java.util.Map;

public class RemoteTransitionMetaData implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String ruleName;
	public Map<String, Double> costs;
}
