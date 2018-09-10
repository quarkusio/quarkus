package org.hibernate.protean.impl;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ServiceLoader;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Replaces the ClassLoaderService in Hibernate ORM with one which should work in Substrate.
 */
public class FlatClassLoaderService implements ClassLoaderService {

	private static final CoreMessageLogger log = CoreLogging.messageLogger( ClassLoaderServiceImpl.class );
	public static final ClassLoaderService INSTANCE = new FlatClassLoaderService();

	private FlatClassLoaderService() {
		//use #INSTANCE when you need one
	}

	@Override
	public <T> Class<T> classForName(String className) {
		try {
			return (Class<T>) Class.forName( className );
		}
		catch (ClassNotFoundException e) {
			log.errorf( "Could not load class '%s' using Class.forName(String)", className );
		}
		return null;
	}

	@Override
	public URL locateResource(String name) {
		URL resource = FlatClassLoaderService.class.getResource( name );
		if ( resource == null ) {
			log.warnf( "Loading of resource '%s' failed. Maybe that's ok, maybe you forgot to include this resource in the binary image? -H:IncludeResources=", name );
		}
		else {
			log.debugf( "Successfully loaded resource '%s'", name );
		}
		return resource;
	}

	@Override
	public InputStream locateResourceStream(String name) {
		InputStream resourceAsStream = FlatClassLoaderService.class.getResourceAsStream( name );
		if ( resourceAsStream == null ) {
			log.warnf( "Loading of resource '%s' failed. Maybe that's ok, maybe you forgot to include this resource in the binary image? -H:IncludeResources=", name );
		}
		else {
			log.debugf( "Successfully loaded resource '%s'", name );
		}
		return resourceAsStream;
	}

	@Override
	public List<URL> locateResources(String name) {
		log.debugf( "locateResources (plural form) was invoked for resource '%s'. Is there a real need for this plural form?", name );
		return Collections.singletonList( locateResource( name ) );
	}

	@Override
	public <S> Collection<S> loadJavaServices(Class<S> serviceContract) {
		ServiceLoader<S> serviceLoader = ServiceLoader.load( serviceContract );
		final LinkedHashSet<S> services = new LinkedHashSet<S>();
		for ( S service : serviceLoader ) {
			services.add( service );
		}
		return services;
	}

	//@Override : not present on all tested branches!
	public <T> T generateProxy(InvocationHandler handler, Class... interfaces) {
		log.error( "Not implemented! generateProxy(InvocationHandler handler, Class... interfaces)" );
		return null;
	}

	@Override
	public <T> T workWithClassLoader(Work<T> work) {
		ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
		return work.doWork( systemClassLoader );
	}

	@Override
	public void stop() {
		//easy!
	}

}
