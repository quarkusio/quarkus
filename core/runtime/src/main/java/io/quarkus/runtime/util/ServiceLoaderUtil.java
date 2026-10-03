package io.quarkus.runtime.util;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Runtime support for build-time ServiceLoader optimization.
 * <p>
 * At build time, the deployment processor resolves which service providers would be found by
 * {@link java.util.ServiceLoader}. At runtime, this class instantiates them using the same
 * approach as {@code ServiceLoader}: {@code Class.forName(name, false, classLoader)} followed
 * by default constructor invocation with {@code setAccessible(true)}.
 */
public final class ServiceLoaderUtil {

    private ServiceLoaderUtil() {
    }

    /**
     * Instantiates pre-resolved service providers using the thread context classloader.
     */
    public static Iterable<?> load(String[] providerClassNames) {
        return load(providerClassNames, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Instantiates pre-resolved service providers.
     */
    public static Iterable<?> load(String[] providerClassNames, ClassLoader classLoader) {
        if (providerClassNames.length == 0) {
            return List.of();
        }
        if (providerClassNames.length == 1) {
            try {
                return List.of(instantiate(providerClassNames[0], classLoader));
            } catch (Exception e) {
                // skip, matching ServiceLoader's behavior on ServiceConfigurationError
                return List.of();
            }
        }

        List<Object> result = new ArrayList<>(providerClassNames.length);
        for (String providerClassName : providerClassNames) {
            try {
                result.add(instantiate(providerClassName, classLoader));
            } catch (Exception e) {
                // skip, matching ServiceLoader's behavior on ServiceConfigurationError
            }
        }
        return result;
    }

    private static Object instantiate(String providerClassName, ClassLoader classLoader) throws Exception {
        Class<?> providerClass = Class.forName(providerClassName, false, classLoader);
        Constructor<?> cons = providerClass.getConstructor();
        cons.setAccessible(true);
        return cons.newInstance();
    }
}
