package io.quarkus.oidc.deployment.devservices.keycloak;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

public final class LightweightDevServicesConfigBuildItem extends SimpleBuildItem {

    private final Map<String, String> config;

    public LightweightDevServicesConfigBuildItem(Map<String, String> config) {
        this.config = config;
    }

    public Map<String, String> getConfig() {
        return config;
    }
}
