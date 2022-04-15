package io.quarkus.oidc.deployment.devservices.keycloak;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

public final class KeycloakDevServicesConfigBuildItem extends SimpleBuildItem {

    private final Map<String, String> config;
    private final Map<String, Object> properties;

    public KeycloakDevServicesConfigBuildItem(Map<String, String> config, Map<String, Object> configProperties) {
        this.config = config;
        this.properties = configProperties;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public Map<String, String> getConfig() {
        return config;
    }
}
