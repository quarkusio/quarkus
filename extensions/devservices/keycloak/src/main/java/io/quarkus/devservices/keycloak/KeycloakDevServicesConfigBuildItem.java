package io.quarkus.devservices.keycloak;

import static io.quarkus.devservices.keycloak.KeycloakDevServicesProcessor.KEYCLOAK_URL_KEY;

import java.util.Map;
import java.util.Optional;

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

    public static String getKeycloakUrl(Optional<KeycloakDevServicesConfigBuildItem> configBuildItem) {
        return configBuildItem
                .map(KeycloakDevServicesConfigBuildItem::getConfig)
                .map(config -> config.get(KEYCLOAK_URL_KEY))
                .orElse(null);
    }
}
