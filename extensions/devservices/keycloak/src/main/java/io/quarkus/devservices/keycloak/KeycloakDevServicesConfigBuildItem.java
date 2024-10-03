package io.quarkus.devservices.keycloak;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

public final class KeycloakDevServicesConfigBuildItem extends SimpleBuildItem {

    private final Map<String, String> config;
    private final Map<String, Object> properties;
    private final boolean containerRestarted;

    public KeycloakDevServicesConfigBuildItem(Map<String, String> config, Map<String, Object> configProperties,
            boolean containerRestarted) {
        this.config = config;
        this.properties = configProperties;
        this.containerRestarted = containerRestarted;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public boolean isContainerRestarted() {
        return containerRestarted;
    }
}
