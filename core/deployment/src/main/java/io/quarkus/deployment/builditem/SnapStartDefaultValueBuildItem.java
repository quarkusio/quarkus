package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Allows extensions to set a default value for enabling SnapStart.
 */
public final class SnapStartDefaultValueBuildItem extends SimpleBuildItem {
    private final boolean defaultValue;

    public SnapStartDefaultValueBuildItem(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isDefaultValue() {
        return defaultValue;
    }
}
