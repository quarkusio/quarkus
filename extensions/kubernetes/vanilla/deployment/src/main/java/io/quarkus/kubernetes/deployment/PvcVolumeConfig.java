package io.quarkus.kubernetes.deployment;

import io.smallrye.config.WithDefault;

public interface PvcVolumeConfig {
    /**
     * The name of the claim to mount.
     */
    String claimName();

    /**
     * Default mode. When specifying an octal number, leading zero must be present.
     */
    @WithDefault("0600")
    String defaultMode();

    /**
     * Optional
     */
    @WithDefault("false")
    boolean optional();
}
