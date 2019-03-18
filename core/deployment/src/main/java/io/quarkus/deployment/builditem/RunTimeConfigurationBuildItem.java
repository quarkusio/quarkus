package io.quarkus.deployment.builditem;

import org.jboss.builder.item.SimpleBuildItem;

import io.quarkus.deployment.configuration.ConfigDefinition;

/**
 * The build item which carries the run time configuration.
 */
public final class RunTimeConfigurationBuildItem extends SimpleBuildItem {
    private final ConfigDefinition configDefinition;

    public RunTimeConfigurationBuildItem(final ConfigDefinition configDefinition) {
        this.configDefinition = configDefinition;
    }

    public ConfigDefinition getConfigDefinition() {
        return configDefinition;
    }
}
