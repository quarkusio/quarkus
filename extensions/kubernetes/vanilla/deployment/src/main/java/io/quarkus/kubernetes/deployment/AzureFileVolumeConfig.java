package io.quarkus.kubernetes.deployment;

import io.smallrye.config.WithDefault;

public interface AzureFileVolumeConfig {
    /**
     * The share name.
     */
    String shareName();

    /**
     * The secret name.
     */
    String secretName();

    /**
     * Whether the volumeName is read only or not.
     */
    @WithDefault("false")
    boolean readOnly();
}
