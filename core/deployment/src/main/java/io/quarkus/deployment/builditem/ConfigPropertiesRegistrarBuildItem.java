package io.quarkus.deployment.builditem;

import java.util.Map;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The {@link org.eclipse.microprofile.config.inject.ConfigProperties} that must be registered with
 * {@link io.smallrye.config.SmallRyeConfig}.
 * <p>
 * While {@link ConfigPropertiesBuildItem} represents each discovered
 * {@link org.eclipse.microprofile.config.inject.ConfigProperties} class, this build item collects only the config
 * properties with active injection points (or marked as unremovable), expanding all prefix overrides for the same
 * config class, since {@link io.smallrye.config.SmallRyeConfig} requires a separate registration for each class and
 * prefix combination.
 * <p>
 * The map key is the fully qualified config class name, and the value is the set of prefixes under which it
 * must be registered.
 *
 * @see ConfigPropertiesBuildItem
 */
public final class ConfigPropertiesRegistrarBuildItem extends SimpleBuildItem {
    private final Map<String, Set<String>> configProperties;

    public ConfigPropertiesRegistrarBuildItem(Map<String, Set<String>> configProperties) {
        this.configProperties = configProperties;
    }

    public Map<String, Set<String>> getConfigProperties() {
        return configProperties;
    }
}
