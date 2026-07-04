package io.quarkus.deployment.builditem;

import java.util.Map;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The {@link io.smallrye.config.ConfigMapping} classes that must be registered with
 * {@link io.smallrye.config.SmallRyeConfig}.
 * <p>
 * While {@link ConfigMappingBuildItem} represents each discovered {@link io.smallrye.config.ConfigMapping} class,
 * this build item collects only the config mappings with active injection points (or marked as unremovable),
 * expanding all prefix overrides for the same config class, since {@link io.smallrye.config.SmallRyeConfig} requires
 * a separate registration for each class and prefix combination.
 * <p>
 * The map key is the fully qualified config class name, and the value is the set of prefixes under which it
 * must be registered.
 *
 * @see ConfigMappingBuildItem
 */
public final class ConfigMappingsRegistrarBuildItem extends SimpleBuildItem {
    private final Map<String, Set<String>> configMappings;

    public ConfigMappingsRegistrarBuildItem(Map<String, Set<String>> configMappings) {
        this.configMappings = configMappings;
    }

    public Map<String, Set<String>> getConfigMappings() {
        return configMappings;
    }
}
