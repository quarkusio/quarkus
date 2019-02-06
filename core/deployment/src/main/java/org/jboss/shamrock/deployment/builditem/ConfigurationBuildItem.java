package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.SimpleBuildItem;
import org.jboss.shamrock.deployment.configuration.ConfigDefinition;

/**
 * The build item which carries the build time configuration.
 */
public final class ConfigurationBuildItem extends SimpleBuildItem {
    private final ConfigDefinition configDefinition;

    public ConfigurationBuildItem(final ConfigDefinition configDefinition) {
        this.configDefinition = configDefinition;
    }

    public ConfigDefinition getConfigDefinition() {
        return configDefinition;
    }
}
