package io.quarkus.smallrye.graphql.runtime.spi;

import graphql.schema.PropertyDataFetcherHelper;
import io.smallrye.graphql.execution.Classes;
import io.smallrye.graphql.spi.ClassloadingService;

/**
 * Quarkus specific classloading service, that allows
 * hot reloading to work in dev mode.
 */
public class QuarkusClassloadingService implements ClassloadingService {

    /*
     * <em>Ugly Hack</em>
     * In Quarkus dev mode, we receive a classloader to use, when doing hot reload
     * This hack is required because using the TCCL would get an outdated version - the initial one.
     * This is because the worker thread on which the handler is called captures the TCCL at creation time
     * and does not allow updating it.
     *
     * In non dev mode, the TCCL is used.
     *
     * TODO: remove this once the vert.x class loader issues are resolved.
     */
    private static volatile ClassLoader classLoader;

    @Override
    public String getName() {
        return "Quarkus";
    }

    @Override
    public Class<?> loadClass(String className) {
        try {
            if (Classes.isPrimitive(className)) {
                return Classes.getPrimativeClassType(className);
            } else {
                ClassLoader cl = classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader;
                return loadClass(className, cl);
            }
        } catch (ClassNotFoundException pae) {
            throw new RuntimeException("Can not load class [" + className + "]", pae);
        }
    }

    public static void setClassLoader(ClassLoader classLoader) {
        QuarkusClassloadingService.classLoader = classLoader;
        PropertyDataFetcherHelper.clearReflectionCache();
    }
}
