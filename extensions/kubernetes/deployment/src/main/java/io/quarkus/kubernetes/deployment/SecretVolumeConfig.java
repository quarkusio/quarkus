package io.quarkus.kubernetes.deployment;

import io.quarkus.runtime.annotations.ConfigItem;

public class SecretVolumeConfig {

    /**
     * The name of the secret to mount.
     */
    String secretName;

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
