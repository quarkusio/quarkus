package io.quarkus.oidc.deployment.devservices.keycloak;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

public final class KeycloakDevServicesConfigBuildItem extends SimpleBuildItem {

    private final Map<String, Object> properties;

    public KeycloakDevServicesConfigBuildItem(Map<String, Object> configProperties) {
        this.properties = configProperties;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
