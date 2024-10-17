package io.quarkus.kubernetes.deployment;

import java.util.Map;

import io.smallrye.config.WithDefault;

public interface SecretVolumeConfig {
    /**
     * The name of the secret to mount.
     */
    String secretName();

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
