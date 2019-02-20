package io.quarkus.deployment.builditem;

import org.jboss.builder.item.SimpleBuildItem;

import io.quarkus.deployment.configuration.ConfigDefinition;

/**
 * The build item which carries the build time configuration that is visible from run time.
 */
public final class BuildTimeRunTimeFixedConfigurationBuildItem extends SimpleBuildItem {
    private final ConfigDefinition configDefinition;

    public BuildTimeRunTimeFixedConfigurationBuildItem(final ConfigDefinition configDefinition) {
        this.configDefinition = configDefinition;
    }

    public ConfigDefinition getConfigDefinition() {
        return configDefinition;
    }
}
