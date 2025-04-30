package io.quarkus.arc.deployment;

import org.jboss.jandex.Type;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.ExecutionMode;

/**
 * Represents a mandatory config property that needs to be validated at runtime.
 */
public final class ConfigPropertyBuildItem extends MultiBuildItem {
    private final String propertyName;
    private final Type propertyType;
    private final String defaultValue;
    private final ExecutionMode executionMode;

    private ConfigPropertyBuildItem(
            final String propertyName,
            final Type propertyType,
            final String defaultValue,
            final ExecutionMode executionMode) {

        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.defaultValue = defaultValue;
        this.executionMode = executionMode;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Type getPropertyType() {
        return propertyType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public boolean isStaticInit() {
        return executionMode.equals(ExecutionMode.STATIC_INIT);
    }

    public boolean isRuntimeInit() {
        return executionMode.equals(ExecutionMode.RUNTIME_INIT);
    }

    public static ConfigPropertyBuildItem staticInit(
            final String propertyName,
            final Type propertyType,
            final String defaultValue) {
        return new ConfigPropertyBuildItem(propertyName, propertyType, defaultValue, ExecutionMode.STATIC_INIT);
    }

    public static ConfigPropertyBuildItem runtimeInit(
            final String propertyName,
            final Type propertyType,
            final String defaultValue) {
        return new ConfigPropertyBuildItem(propertyName, propertyType, defaultValue, ExecutionMode.RUNTIME_INIT);
    }
}
