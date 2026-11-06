package io.quarkus.spring.boot.properties.deployment;

import java.util.Map;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ConfigurationPropertiesRegistrarBuildItem extends SimpleBuildItem {
    private final Map<String, Set<String>> configMappings;

    public ConfigurationPropertiesRegistrarBuildItem(Map<String, Set<String>> configMappings) {
        this.configMappings = configMappings;
    }

    public Map<String, Set<String>> getConfigMappings() {
        return configMappings;
    }
}
