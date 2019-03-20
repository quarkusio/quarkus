package io.quarkus.arc.deployment;

import org.jboss.builder.item.MultiBuildItem;

/**
 * Represents a mandatory config property that needs to be validated at runtime.
 */
public final class ConfigPropertyBuildItem extends MultiBuildItem {

    private final String propertyName;

    private final Class<?> propertyType;

    public ConfigPropertyBuildItem(String propertyName, Class<?> propertyType) {
        this.propertyName = propertyName;
        this.propertyType = propertyType;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Class<?> getPropertyType() {
        return propertyType;
    }

}
