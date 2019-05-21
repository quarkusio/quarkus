package io.quarkus.arc.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a mandatory config property that needs to be validated at runtime.
 */
public final class ConfigPropertyBuildItem extends MultiBuildItem {

    private final String propertyName;

    private final String propertyType;

    public ConfigPropertyBuildItem(String propertyName, String propertyType) {
        this.propertyName = propertyName;
        this.propertyType = propertyType;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getPropertyType() {
        return propertyType;
    }

}
