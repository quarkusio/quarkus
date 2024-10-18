package io.quarkus.kubernetes.deployment;

import java.util.Map;

import io.smallrye.config.WithDefault;

public interface ConfigMapVolumeConfig {
    /**
     * The name of the ConfigMap to mount.
     */
    String configMapName();

    /**
     * Default mode. When specifying an octal number, leading zero must be present.
     */
    @WithDefault("0600")
    String defaultMode();

    /**
     * The list of files to be mounted.
     */
    Map<String, VolumeItemConfig> items();

    /**
     * Optional
     */
    @WithDefault("false")
    boolean optional();
}
