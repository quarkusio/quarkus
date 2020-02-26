package io.quarkus.kubernetes.deployment;

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
     *
     * @return The default mode.
     */
    @ConfigItem(defaultValue = "0600")
    Integer defaultMode;

    /**
     * Optional
     */
    @ConfigItem(defaultValue = "false")
    boolean optional;

}
