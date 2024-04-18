package io.quarkus.hibernate.orm.deployment.integration;

import java.io.IOException;

import io.quarkus.deployment.util.IoUtil;
import net.bytebuddy.dynamic.ClassFileLocator;

/**
 * Custom implementation of a ClassFileLocator which will load resources
 * from the context classloader which is set at the time of the locate()
 * operation is being performed.
 * Using a regular ForClassLoader implementation would capture the currently
 * set ClassLoader and keep a reference to it, while we need it to look
 * for a fresh copy during the enhancement.
 * Additionally, we might be able to optimize how the resource is actually
 * being loaded as we control the ClassLoader implementations
 * (Such further optimisations are not implemented yet).
 */
public final class QuarkusClassFileLocator implements ClassFileLocator {

    public static final QuarkusClassFileLocator INSTANCE = new QuarkusClassFileLocator();

    private QuarkusClassFileLocator() {
        //do not invoke, use INSTANCE
    }

    @Override
    public Resolution locate(final String name) throws IOException {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final byte[] bytes = IoUtil.readClassAsBytes(classLoader, name);
        if (bytes != null) {
            return new Resolution.Explicit(bytes);
        } else {
            return new Resolution.Illegal(name);
        }
    }

    @Override
    public void close() {
        //nothing to do
    }

}
