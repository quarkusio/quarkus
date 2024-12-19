package io.quarkus.devservices.oidc.lightweight;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * OIDC Lightweight Dev Services configuration properties.
 */
public final class OidcLightweightDevServicesConfigBuildItem extends SimpleBuildItem {

    private final Map<String, String> config;

    OidcLightweightDevServicesConfigBuildItem(Map<String, String> config) {
        this.config = config;
    }

    public Map<String, String> getConfig() {
        return config;
    }

}
