
package io.quarkus.kubernetes.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class AzureFileVolumeConfig {

    /**
     * The share name.
     */
    @ConfigItem
    String shareName;

    /**
     * The secret name.
     */
    @ConfigItem
    String secretName;

    /**
     * Wether the volumeName is read only or not.
     */
    @ConfigItem
    boolean readOnly;

}
