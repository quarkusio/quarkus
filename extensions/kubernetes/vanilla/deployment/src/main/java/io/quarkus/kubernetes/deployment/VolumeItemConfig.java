package io.quarkus.kubernetes.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class VolumeItemConfig {
    /**
     * The path where the file will be mounted.
     */
    @ConfigItem
    String path;

    /**
     * It must be a value between 0000 and 0777. If not specified, the volume defaultMode will be used.
     */
    @ConfigItem(defaultValue = "-1")
    int mode;
}
