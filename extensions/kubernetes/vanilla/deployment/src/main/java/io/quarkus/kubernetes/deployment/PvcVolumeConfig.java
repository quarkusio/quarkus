
package io.quarkus.kubernetes.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class PvcVolumeConfig {

    /**
     * The name of the claim to mount.
     */
    @ConfigItem
    String claimName;

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
