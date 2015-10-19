package org.eclipse.viatra.dse.cluster.node;

import java.io.Serializable;

public class PatternDefinition implements Serializable {
	private final String querySpecificationName;

	private final String name;

	public PatternDefinition(String querySpecificationName, String name) {
		super();
		this.querySpecificationName = querySpecificationName;
		this.name = name;
	}

	public String getQuerySpecificationName() {
		return querySpecificationName;
	}

	public String getName() {
		return name;
	}
}
