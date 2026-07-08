package io.quarkus.core.deployment.action.impl;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Utility for detecting {@code @ConfigMapping} + {@code @ConfigRoot} interfaces.
 * Shared between {@link ConfigCaptureInterceptor} and {@link ServiceBuilderImpl}.
 */
final class ConfigMappingDetector {

    private ConfigMappingDetector() {
    }

    /**
     * Find the {@code @ConfigMapping}-annotated interface implemented by the given class.
     * Walks the class hierarchy and all implemented interfaces.
     *
     * @param clazz the class to inspect
     * @return the {@code @ConfigMapping}-annotated interface, or {@code null} if none found
     */
    static Class<?> findConfigMappingInterface(Class<?> clazz) {
        if (clazz.isInterface() && clazz.isAnnotationPresent(ConfigMapping.class)) {
            return clazz;
        }
        for (Class<?> iface : clazz.getInterfaces()) {
            if (iface.isAnnotationPresent(ConfigMapping.class)) {
                return iface;
            }
            Class<?> found = findConfigMappingInterface(iface);
            if (found != null) {
                return found;
            }
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            return findConfigMappingInterface(superclass);
        }
        return null;
    }

    /**
     * Find the {@code @ConfigRoot} annotation on the config mapping interface for the given type.
     * Returns {@code null} if the type is not a config mapping or has no {@code @ConfigRoot}.
     *
     * @param type the type to check
     * @return the {@code @ConfigRoot} annotation, or {@code null}
     */
    static ConfigRoot findConfigRoot(Class<?> type) {
        Class<?> configInterface = findConfigMappingInterface(type);
        if (configInterface == null) {
            return null;
        }
        return configInterface.getAnnotation(ConfigRoot.class);
    }
}
