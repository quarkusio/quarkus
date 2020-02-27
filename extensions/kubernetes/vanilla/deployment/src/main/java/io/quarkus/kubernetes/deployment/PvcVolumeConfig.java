
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
