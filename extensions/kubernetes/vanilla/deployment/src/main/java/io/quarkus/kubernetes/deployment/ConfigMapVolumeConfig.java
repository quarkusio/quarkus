package io.quarkus.kubernetes.deployment;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ConfigMapVolumeConfig {

    /**
     * The name of the ConfigMap to mount.
     */
    @ConfigItem
    String configMapName;

    /**
     * Default mode.
     * When specifying an octal number, leading zero must be present.
     *
     * @return The default mode.
     */
    @ConfigItem(defaultValue = "0600")
    String defaultMode;

    /**
     * The list of files to be mounted.
     */
    @ConfigItem
    Map<String, VolumeItemConfig> items;

    /**
     * Optional
     */
    @ConfigItem
    boolean optional;

}
