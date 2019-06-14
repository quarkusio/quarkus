package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a system property that will be set immediately on application startup.
 *
 */
public final class SystemPropertyBuildItem extends MultiBuildItem {

    private final String key;
    private final String value;

    public SystemPropertyBuildItem(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
