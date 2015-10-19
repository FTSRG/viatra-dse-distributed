package org.eclipse.viatra.dse.cluster.host;

public class ClusterConfig {
	private final String serviceIP;
	private final String servicePort;

	public ClusterConfig(String serviceIP, String servicePort) {
		super();
		this.serviceIP = serviceIP;
		this.servicePort = servicePort;
	}

	public static ClusterConfig defaultConfig() {
		return new ClusterConfig("localhost", "2553");
	}

	public String getServiceIP() {
		return serviceIP;
	}

	public String getServicePort() {
		return servicePort;
	}
}
