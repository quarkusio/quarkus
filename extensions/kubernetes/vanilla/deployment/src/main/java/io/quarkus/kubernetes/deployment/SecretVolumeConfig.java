package io.quarkus.kubernetes.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class SecretVolumeConfig {

    /**
     * The name of the secret to mount.
     */
    @ConfigItem
    String secretName;

    /**
     * Default mode.
     * When specifying an octal number, leading zero must be present.
     *
     * @return The default mode.
     */
    @ConfigItem(defaultValue = "0600")
    String defaultMode;

    /**
     * Optional
     */
    @ConfigItem
    boolean optional;
}
