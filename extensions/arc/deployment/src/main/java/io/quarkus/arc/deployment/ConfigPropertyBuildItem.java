package io.quarkus.arc.deployment;

import org.jboss.jandex.Type;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a mandatory config property that needs to be validated at runtime.
 */
public final class ConfigPropertyBuildItem extends MultiBuildItem {
    private final String propertyName;
    private final Type propertyType;
    private final String defaultValue;

    public ConfigPropertyBuildItem(final String propertyName, final Type propertyType, final String defaultValue) {
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.defaultValue = defaultValue;
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
}
