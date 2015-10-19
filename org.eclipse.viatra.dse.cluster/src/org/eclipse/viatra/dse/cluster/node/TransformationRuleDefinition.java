package org.eclipse.viatra.dse.cluster.node;

import java.io.Serializable;

public class TransformationRuleDefinition implements Serializable {

	private final String leftHandSideName;
	private final String rightHandSideName;

	public TransformationRuleDefinition(String leftHandSideName, String rightHandSideName) {
		super();
		this.leftHandSideName = leftHandSideName;
		this.rightHandSideName = rightHandSideName;
	}

	public String getLeftHandSideName() {
		return leftHandSideName;
	}

	public String getRightHandSideName() {
		return rightHandSideName;
	}

}
