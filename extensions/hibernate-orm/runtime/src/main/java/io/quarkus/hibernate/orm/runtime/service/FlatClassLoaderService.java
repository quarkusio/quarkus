package io.quarkus.hibernate.orm.runtime.service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ServiceLoader;

import org.hibernate.AssertionFailure;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Replaces the ClassLoaderService in Hibernate ORM with one which should work in native mode.
 */
public class FlatClassLoaderService implements ClassLoaderService {

    private static final CoreMessageLogger log = CoreLogging.messageLogger(FlatClassLoaderService.class);
    public static final ClassLoaderService INSTANCE = new FlatClassLoaderService();

    private FlatClassLoaderService() {
        // use #INSTANCE when you need one
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Class<T> classForName(String className) {
        try {
            return (Class<T>) Class.forName(className, false, getClassLoader());
        } catch (Exception | LinkageError e) {
            throw new ClassLoadingException("Unable to load class [" + className + "]", e);
        }
    }

    @Override
    public URL locateResource(String name) {
        URL resource = getClassLoader().getResource(name);
        if (resource == null) {
            log.debugf(
                    "Loading of resource '%s' failed. Maybe that's ok, maybe you forgot to include this resource in the binary image? -H:IncludeResources=",
                    name);
        } else {
            log.tracef("Successfully loaded resource '%s'", name);
        }
        return resource;
    }

    @Override
    public InputStream locateResourceStream(String name) {
        InputStream resourceAsStream = getClassLoader().getResourceAsStream(name);
        if (resourceAsStream == null) {
            log.debugf(
                    "Loading of resource '%s' failed. Maybe that's ok, maybe you forgot to include this resource in the binary image? -H:IncludeResources=",
                    name);
        } else {
            log.tracef("Successfully loaded resource '%s'", name);
        }
        return resourceAsStream;
    }

    @Override
    public List<URL> locateResources(String name) {
        log.debugf(
                "locateResources (plural form) was invoked for resource '%s'. Is there a real need for this plural form?",
                name);
        try {
            Enumeration<URL> resources = getClassLoader().getResources(name);
            List<URL> resource = new ArrayList<>();
            while (resources.hasMoreElements()) {
                resource.add(resources.nextElement());
            }
            return resource;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <S> Collection<S> loadJavaServices(Class<S> serviceContract) {
        ServiceLoader<S> serviceLoader = ServiceLoader.load(serviceContract, getClassLoader());
        final LinkedHashSet<S> services = new LinkedHashSet<S>();
        for (S service : serviceLoader) {
            services.add(service);
        }
        return services;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public <T> T generateProxy(InvocationHandler handler, Class... interfaces) {
        throw new AssertionFailure("Not implemented! generateProxy(InvocationHandler handler, Class... interfaces)");
    }

    @Override
    public Package packageForNameOrNull(String packageName) {
        try {
            Class<?> aClass = Class.forName(packageName + ".package-info", false, getClassLoader());
            return aClass == null ? null : aClass.getPackage();
        } catch (ClassNotFoundException e) {
            log.packageNotFound(packageName);
            return null;
        } catch (LinkageError e) {
            log.warn("LinkageError while attempting to load Package named " + packageName, e);
            return null;
        }
    }

    @Override
    public <T> T workWithClassLoader(Work<T> work) {
        ClassLoader systemClassLoader = getClassLoader();
        return work.doWork(systemClassLoader);
    }

    @Override
    public void stop() {
        // easy!
    }

    private ClassLoader getClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            return FlatClassLoaderService.class.getClassLoader();
        }
        return cl;
    }

}
