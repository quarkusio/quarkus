package io.quarkus.devservices.crossclassloader.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.ConfigMapping;

/**
 * @param globalConfig should be a io.quarkus.deployment.dev.devservices.DevServicesConfig, but is not that type to avoid
 *        the dependency on the devservices module
 * @param identifyingConfig a config object specific to the extension's dev services configuration
 */
public record ComparableDevServicesConfig(UUID applicationInstanceId,
        DevServiceOwner owner,
        Object globalConfig,
        Object identifyingConfig) {

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ComparableDevServicesConfig that))
            return false;
        return Objects.equals(owner, that.owner)
                && reflectiveEquals(globalConfig, that.globalConfig)
                && reflectiveEquals(identifyingConfig, that.identifyingConfig)
                && Objects.equals(applicationInstanceId, that.applicationInstanceId);
    }

    private static boolean reflectiveEquals(Object config, Object otherConfig) {

        // This could be expensive, so we might wish to shove everything into a map, reflectively, and just compare that
        // The externals won't change if we do that, so we can do it later if we want to
        // We can assume config mapping is immutable
        // We need to compare across classloaders, so we cannot use equals()

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

                    if (isConfigInterface(iface)) {
                        // For each method in the interface
                        // In the future, if we wanted some methods to be ignored, we could use a marker annotation in the config object
                        for (Method method : iface.getMethods()) {

                            int modifiers = method.getModifiers();
                            if (isInvokableMethod(method, modifiers)) {
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
                }
                clazz = clazz.getSuperclass();
                otherClazz = otherClazz.getSuperclass();

            }
            return true;
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * We can't just use clazz.isAnnotationPresent(), because the annotation itself is likely to be proxied
     */
    private static boolean containsAnnotation(Class<?> iface, Class<? extends Annotation> configAnnotation) {
        return Arrays.stream(iface.getAnnotations())
                .anyMatch(a -> a.annotationType().getName().equals(configAnnotation.getName()));
    }

    private static boolean isInvokableMethod(Method method, int modifiers) {
        if (Modifier.isStatic(modifiers) ||
                Modifier.isTransient(modifiers)) {
            return false;
        }

        if (Modifier.isPrivate(modifiers)) {
            return false;
        }

        if (method.getParameterCount() > 0) {
            return false;
        }

        method.setAccessible(true);
        return true;
    }

    private static boolean isConfigInterface(Class<?> iface) {
        return containsAnnotation(iface, ConfigMapping.class) || containsAnnotation(iface, ConfigGroup.class);
    }

}
