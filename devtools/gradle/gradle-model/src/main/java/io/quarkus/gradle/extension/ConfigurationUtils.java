package io.quarkus.gradle.extension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.gradle.api.GradleException;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.jetbrains.annotations.NotNull;

// This is necessary because in included builds the returned `Project` instance and
// `QuarkusExtensionConfiguration` extension is provided by a different class loader
// which prevents us from creating some interface or casting to it directly.
public class ConfigurationUtils {
    private static Object callGetter(@NotNull Object extensionConfiguration, String getterName) {
        final Method getterMethod;

        try {
            getterMethod = extensionConfiguration.getClass().getMethod(getterName);
        } catch (NoSuchMethodException e) {
            throw new GradleException(
                    "Didn't find method " + getterName + " on class " + extensionConfiguration.getClass().getName(), e);
        }

        try {
            return getterMethod.invoke(extensionConfiguration);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new GradleException(
                    "Failed to call method " + getterName + " on class " + extensionConfiguration.getClass().getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Property<String> getDeploymentModule(@NotNull Object extensionConfiguration) {
        return (Property<String>) callGetter(extensionConfiguration, "getDeploymentModule");
    }

    @SuppressWarnings("unchecked")
    public static ListProperty<String> getConditionalDependencies(@NotNull Object extensionConfiguration) {
        return (ListProperty<String>) callGetter(extensionConfiguration, "getConditionalDependencies");
    }

    @SuppressWarnings("unchecked")
    public static ListProperty<String> getDependencyConditions(@NotNull Object extensionConfiguration) {
        return (ListProperty<String>) callGetter(extensionConfiguration, "getDependencyConditions");
    }
}
