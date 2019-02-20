package io.quarkus.deployment.builditem;

import org.jboss.builder.item.SimpleBuildItem;

import io.quarkus.deployment.configuration.ConfigDefinition;

/**
 * The build item which carries the build time configuration.
 */
public final class BuildTimeConfigurationBuildItem extends SimpleBuildItem {
    private final ConfigDefinition configDefinition;

    public BuildTimeConfigurationBuildItem(final ConfigDefinition configDefinition) {
        this.configDefinition = configDefinition;
    }

    public ConfigDefinition getConfigDefinition() {
        return configDefinition;
    }
}
