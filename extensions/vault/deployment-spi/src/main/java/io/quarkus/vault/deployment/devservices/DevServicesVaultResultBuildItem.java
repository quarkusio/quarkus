package io.quarkus.vault.deployment.devservices;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

public final class DevServicesVaultResultBuildItem extends SimpleBuildItem {
    private final Map<String, String> properties;

    public DevServicesVaultResultBuildItem(Map<String, String> properties) {
        this.properties = properties;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
