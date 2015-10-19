package org.eclipse.viatra.dse.cluster.node;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.apache.log4j.Logger;
import org.eclipse.viatra.dse.cluster.interfaces.IProblemServer;

/**
 * This is the classloader on the worker nodes that load the received bytecode
 * into memory.
 * 
 * @author Miki
 * 
 */
public class RemoteAkkaClassloader extends ClassLoader {

	private static Logger log = Logger.getLogger(RemoteAkkaClassloader.class.getName());
	
	/**
	 * The problem node local stub.
	 */
	private IProblemServer remoteClassLoaderActor;

	/**
	 * The parent classloader.
	 */
	private final ClassLoader parent;

	/**
	 * Default constructor with no specified parent classloader.
	 */
	private RemoteAkkaClassloader() {
		this(null);
	}

	/**
	 * Constructor with supplied parent class loader.
	 * 
	 * @param parent
	 */
	public RemoteAkkaClassloader(ClassLoader parent) {
		super(parent);
		this.parent = parent;
	}

	public IProblemServer getRemoteClassLoaderActor() {
		return remoteClassLoaderActor;
	}

	public void setRemoteClassLoaderActor(IProblemServer remoteClassLoaderActor) {
		this.remoteClassLoaderActor = remoteClassLoaderActor;
	}

	@Override
	public URL getResource(String name) {
		return super.getResource(name);
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		return super.getResourceAsStream(name);
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		return super.getResources(name);
	}

	/**
	 * <p>
	 * This method resolves the requested class. It first tries the default Java
	 * method of classloading, so that it does not try to resolve already loaded
	 * classes.
	 * </p>
	 * 
	 * <p>
	 * If this method fails, it first tries to delegate the classloading to the
	 * OSGi container's appropriate classloader.
	 * </p>
	 * 
	 * <p>
	 * If both the above methods fail to resolve and load the class defined by
	 * the {@code param} parameter, then an attempt is made to load it from the
	 * remote host (the problem's originating host) via the Akka interface.
	 * </p>
	 * 
	 * @exception ClassNotFoundException
	 *                This exception is thrown if all three methods of
	 *                classloading fail.
	 */
	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		try {
			return super.loadClass(name);
		} catch (ClassNotFoundException e) {
			try {
				return this.getClass().getClassLoader().loadClass(name);
			} catch (ClassNotFoundException e2) {
				try {
					return loadClassFromAkka(name);
				} catch (ClassNotFoundException e3) {
					throw (new java.lang.ClassNotFoundException("Local and remote classloading has failed for class: "
							+ name));
				}
			}
		}
	}

	private Class<?> loadClassFromAkka(String name) throws ClassNotFoundException {
		if (remoteClassLoaderActor == null) {
			throw (new java.lang.ClassNotFoundException("No remote classloader defined."));
		}
		try {
			byte[] implementation = remoteClassLoaderActor.loadClass(name);
			defineClass(name, implementation, 0, implementation.length);
			log.info("Loaded remote class: " + name);
			return loadClass(name);
		} catch (Exception e1) {
			throw new ClassNotFoundException("Remote classloading has failed.");
		}
	}

	@Override
	public void setClassAssertionStatus(String className, boolean enabled) {
		super.setClassAssertionStatus(className, enabled);
	}

	@Override
	public void setDefaultAssertionStatus(boolean enabled) {
		super.setDefaultAssertionStatus(enabled);
	}

	@Override
	public void setPackageAssertionStatus(String packageName, boolean enabled) {
		super.setPackageAssertionStatus(packageName, enabled);
	}

	@Override
	public void clearAssertionStatus() {
		super.clearAssertionStatus();
	}
}