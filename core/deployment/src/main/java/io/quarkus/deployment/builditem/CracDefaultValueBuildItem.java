package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Allows extensions to set default value for enabling CRAC
 */
public final class CracDefaultValueBuildItem extends SimpleBuildItem {
    private final boolean defaultValue;

    public CracDefaultValueBuildItem(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isDefaultValue() {
        return defaultValue;
    }
}
