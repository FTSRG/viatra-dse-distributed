package org.eclipse.viatra.dse.cluster.node;

import org.apache.log4j.Logger;
import org.eclipse.viatra.dse.cluster.interfaces.IProblemServer;
import org.eclipse.viatra.dse.cluster.interfaces.IProcessingClient;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;

import akka.actor.ActorSystem;
import akka.actor.TypedActor;
import akka.actor.TypedProps;
import akka.japi.Creator;

/**
 * 
 * -Djava.system.class.loader=org.eclipse.viatra.dse.clustering.node.
 * RemoteAkkaClassloader Starts a worker node.
 * 
 * @author Miki
 * 
 */
public class ClientApplication {
	private static final Logger log = Logger.getLogger(ClientApplication.class.getName());

	/**
	 * The Worker's actor system
	 */
	private ActorSystem system;

	/**
	 * The local DSE node's interface
	 */
	private IProcessingClient node;

//	/**
//	 * Main entry point.
//	 * 
//	 * @param args
//	 *            unused
//	 */
//	public static void main(String[] args) {
//		String localIP = null;
//		DSEClusterApplication instance = new DSEClusterApplication();
//		Enumeration<NetworkInterface> networkInterfaces;
//		try {
//			networkInterfaces = NetworkInterface.getNetworkInterfaces();
//		} catch (SocketException e) {
//			e.printStackTrace();
//			return;
//		}
//		while (networkInterfaces.hasMoreElements()) {
//			NetworkInterface networkInterface = networkInterfaces.nextElement();
//			Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
//			while (inetAddresses.hasMoreElements()) {
//				String hostAddress = inetAddresses.nextElement().getHostAddress();
//				if (hostAddress.startsWith(preferr)) {
//					localIP = hostAddress;
//					log.info("Using local IP: " + localIP);
//				}
//			}
//		}
//		instance.start(localIP);
//	}

	public void start(String serviceIP, String servicePort) {

		ClassLoader clazz = this.getClass().getClassLoader();

		Config appConfig = ConfigFactory.load(clazz, "application.conf", ConfigParseOptions.defaults().setAllowMissing(false), ConfigResolveOptions.defaults());
		Config referenceActorConfig = ConfigFactory.load(clazz, "reference-actor.conf", ConfigParseOptions.defaults().setAllowMissing(false), ConfigResolveOptions.defaults());
		Config referenceRemoteConfig = ConfigFactory.load(clazz, "reference-remote.conf", ConfigParseOptions.defaults().setAllowMissing(false), ConfigResolveOptions.defaults());
		appConfig = appConfig.withFallback(referenceRemoteConfig).withFallback(referenceActorConfig);
		
		Config debugConfig = ConfigFactory.parseString("akka.loglevel = \"DEBUG\"");
		Config portConfig = ConfigFactory.parseString("akka.remote.netty.tcp.port = "+servicePort);
		Config hostConfig = ConfigFactory.parseString("akka.remote.netty.tcp.hostname = "+serviceIP);

		appConfig = hostConfig.withFallback(portConfig).withFallback(debugConfig).withFallback(appConfig);

		// create ActorSystem
		system = ActorSystem.create(IProblemServer.ACTOR_SYSTEM_NAME, appConfig, clazz);
		
		// create the listener node
		node = TypedActor.get(system).typedActorOf(new TypedProps<Client>(IProcessingClient.class, new Creator<Client>() {
			private static final long serialVersionUID = 1L;

			public Client create() {
				try {
					return new Client(system);
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
					throw new Error("Class loader hack has failed. Running is not possible if this hack is not working.", e);
				}
			}
		}), IProblemServer.REMOTE_NODE_NAME);
	}

}
