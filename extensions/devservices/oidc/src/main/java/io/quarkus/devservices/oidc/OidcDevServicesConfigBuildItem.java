package io.quarkus.devservices.oidc;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * OIDC Dev Services configuration properties.
 */
public final class OidcDevServicesConfigBuildItem extends SimpleBuildItem {

    private final Map<String, String> config;

    OidcDevServicesConfigBuildItem(Map<String, String> config) {
        this.config = config;
    }

    public Map<String, String> getConfig() {
        return config;
    }

}
