package io.quarkus.devservices.crossclassloader.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.ConfigMapping;

public class ComparableDevServicesConfig {

    public static final int PRIME_NUMBER = 31;
    private final DevServiceOwner owner;
    private final Object globalConfig;
    private final Object identifyingConfig;

    /**
     * @param globalConfig should be a io.quarkus.deployment.dev.devservices.DevServicesConfig, but is not that type to avoid
     *        the dependency on the devservices module
     * @param identifyingConfig a config object specific to the extension's dev services configuration
     */
    public ComparableDevServicesConfig(DevServiceOwner owner, Object globalConfig, Object identifyingConfig) {
        this.owner = owner;
        this.globalConfig = globalConfig;
        this.identifyingConfig = identifyingConfig;

        // TODO validate types, cache interfaces
    }

    public DevServiceOwner getDevServicesOwner() {
        return owner;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        // Instanceof checks are safe because this package always loads parent-first
        if (!(other instanceof ComparableDevServicesConfig otherObj)) {
            return false;
        }

        DevServiceOwner otherOwner = otherObj.getDevServicesOwner();
        if (!Objects.equals(owner, otherOwner)) {
            return false;
        }

        Object otherWrapped = otherObj.identifyingConfig;
        if (!reflectiveEquals(identifyingConfig, otherWrapped)) {
            return false;
        }

        // Check global config second, since it's more likely to be the same between the objects
        Object otherGlobal = otherObj.globalConfig;
        if (!reflectiveEquals(globalConfig, otherGlobal)) {
            return false;
        }

        return true;

    }

    private static boolean reflectiveEquals(Object config, Object otherConfig) {

        // This could be expensive, so we might wish to shove everything into a map, reflectively, and just compare that
        // The externals won't change if we do that, so we can do it later if we want to

        if (config == null || otherConfig == null) {
            // If they're both null, they're equal
            return config == otherConfig;
        }

        Class<?> clazz = config.getClass();
        Class<?> otherClazz = otherConfig.getClass();

        // We can't compare classes because of multiple classloaders, but we can compare class names
        if (!clazz.getName().equals(otherClazz.getName())) {
            return false;
        }

        try {
            while (clazz != null) {
                // Get all interfaces implemented by the class
                for (Class<?> iface : clazz.getInterfaces()) {
                    // Check if the interface is a config one

                    if (containsAnnotation(iface, ConfigMapping.class) || containsAnnotation(iface, ConfigGroup.class)) {
                        // For each method in the interface
                        for (Method method : iface.getMethods()) {

                            int modifiers = method.getModifiers();
                            if (java.lang.reflect.Modifier.isStatic(modifiers) ||
                                    java.lang.reflect.Modifier.isTransient(modifiers)) {
                                continue; // Skip static and transient methods
                            }

                            if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
                                continue; // Skip private methods, which wouldn't be exposed as config
                            }

                            if (method.getParameterCount() > 0) {
                                continue; // Skip methods which take an argument
                            }

                            method.setAccessible(true);

                            Method otherMethod = clazz == otherClazz ? method : otherClazz.getMethod(method.getName());
                            otherMethod.setAccessible(true);

                            Object thisValue = method.invoke(config);
                            Object otherValue = otherMethod.invoke(otherConfig);

                            // Assume the objects in the config use types from the parent classloader, or we just declare them non-equal if they're not in the same classloader

                            if (!Objects.deepEquals(thisValue, otherValue)) {
                                return false;
                            }
                        }
                    }
                }
                clazz = clazz.getSuperclass();
                otherClazz = otherClazz.getClass();

            }
            return true;
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * We can't just use clazz.isAnnotationPresent(), because the annotation itself is likely to be proxied
     *
     * @param iface
     * @param configAnnotation
     * @return
     */
    private static boolean containsAnnotation(Class<?> iface, Class<? extends Annotation> configAnnotation) {
        return Arrays.stream(iface.getAnnotations())
                .anyMatch(a -> a.annotationType().getName().equals(configAnnotation.getName()));
    }

    @Override
    public int hashCode() {
        int result = owner != null ? owner.hashCode() : 0;
        result = PRIME_NUMBER * result + reflectiveHashCode(identifyingConfig);
        result = PRIME_NUMBER * result + reflectiveHashCode(globalConfig);

        return result;
    }

    private static int reflectiveHashCode(Object config) {
        // It might be wise to cache this, since the reflection will be expensive and config values should not change
        if (config == null) {
            return 0;
        }

        int result = 1;
        Class<?> clazz = config.getClass();
        try {
            while (clazz != null) {
                // Get all interfaces implemented by the class
                for (Class<?> iface : clazz.getInterfaces()) {
                    // Check if the interface is a config one

                    if (containsAnnotation(iface, ConfigMapping.class) || containsAnnotation(iface, ConfigGroup.class)) {
                        // For each method in the interface
                        for (Method method : iface.getMethods()) {

                            final int modifiers = method.getModifiers();
                            if (java.lang.reflect.Modifier.isStatic(modifiers) ||
                                    java.lang.reflect.Modifier.isTransient(modifiers)) {
                                continue; // Skip static and transient methods
                            }

                            if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
                                continue; // Skip private methods, which wouldn't be exposed as config
                            }

                            if (method.getParameterCount() > 0) {
                                continue; // Skip methods which take an argument
                            }

                            method.setAccessible(true);

                            Object thisValue = method.invoke(config);
                            if (thisValue != null) {
                                result = 31 * result + thisValue.hashCode();
                            }
                        }
                    }
                }
                clazz = clazz.getSuperclass();

            }

            return result;
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "ComparableDevServicesConfig[" + owner + "-" + globalConfig + "-" + identifyingConfig + "]";
    }

}
