package org.eclipse.viatra.dse.cluster.node;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import scala.Application;

public class StandaloneClient implements IApplication {

	private String preferredIPMask = "192.168.";
	private String preferredPort = "2552";

	public void setPreferredIP(String preferredIP) {
		this.preferredIPMask = preferredIP;
	}

	public void setPreferredPort(String preferredPort) {
		this.preferredPort = preferredPort;
	}
	
	private static final String DEFAULT_SERVICE_IP = "127.0.0.1";

	private static final Logger log = Logger.getLogger(Application.class.getName());

	@Override
	public Object start(IApplicationContext context) throws Exception {
		System.out.println("DSE CLIENT Started");
		log.getRootLogger().setLevel(Level.DEBUG);
		ClientApplication instance = new ClientApplication();

		Enumeration<NetworkInterface> networkInterfaces;
		try {
			networkInterfaces = NetworkInterface.getNetworkInterfaces();
			String serviceIP = DEFAULT_SERVICE_IP;
			while (networkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = networkInterfaces.nextElement();
				Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
				String address = null;
				while (inetAddresses.hasMoreElements()) {
					String hostAddress = inetAddresses.nextElement().getHostAddress();
					if (hostAddress.startsWith(preferredIPMask)) {
						address = hostAddress;
						break;
					}
				}
				if (address != null) {
					serviceIP = address;
					break;
				}
			}
			log.info("Using IP: " + serviceIP);
			instance.start(serviceIP, preferredPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}

		while (true) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				break;
			}
		}

		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
		System.out.println("My app just stopped");
	}

}
