package io.quarkus.info.deployment.spi;

import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Allows for extensions to include their properties into the info endpoint
 */
public final class InfoBuildTimeValuesBuildItem extends MultiBuildItem {

    private final String name;
    private final Map<String, Object> value;

    public InfoBuildTimeValuesBuildItem(String name, Map<String, Object> value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getValue() {
        return value;
    }
}
