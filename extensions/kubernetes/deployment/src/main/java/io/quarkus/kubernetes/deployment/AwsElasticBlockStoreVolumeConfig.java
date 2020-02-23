
package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class AwsElasticBlockStoreVolumeConfig {

    /**
     * The name of the disk to mount.
     */
    @ConfigItem
    String volumeId;

    /**
     * The partition.
     */
    @ConfigItem
    Optional<Integer> partition;

    /**
     * Filesystem type.
     */
    @ConfigItem(defaultValue = "ext4")
    String fsType;

    /**
     * Wether the volumeName is read only or not.
     */
    @ConfigItem(defaultValue = "false")
    boolean readOnly;

}
