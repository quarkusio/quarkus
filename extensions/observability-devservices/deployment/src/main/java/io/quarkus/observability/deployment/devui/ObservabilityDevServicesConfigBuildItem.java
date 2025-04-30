package io.quarkus.observability.deployment.devui;

import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item used to carry running DevService values to Dev UI.
 */
public final class ObservabilityDevServicesConfigBuildItem extends MultiBuildItem {

    private final Map<String, String> config;

    public ObservabilityDevServicesConfigBuildItem(Map<String, String> config) {
        this.config = config;
    }

    public Map<String, String> getConfig() {
        return config;
    }

}