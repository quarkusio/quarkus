
package io.quarkus.kubernetes.deployment;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class AzureDiskVolumeConfig {

    public enum CachingMode {
        ReadWrite,
        ReadOnly,
        None
    }

    public enum Kind {
        Managed,
        Shared
    }

    /**
     * The name of the disk to mount.
     */
    @ConfigItem
    String diskName;

    /**
     * The URI of the vhd blob object OR the resourceID of an Azure managed data disk if Kind is Managed
     */
    @ConfigItem
    String diskURI;

    /**
     * Kind of disk.
     */
    @ConfigItem(defaultValue = "Managed")
    Kind kind;

    /**
     * Disk caching mode.
     */
    @ConfigItem(defaultValue = "ReadWrite")
    CachingMode cachingMode;

    /**
     * File system type.
     */
    @ConfigItem(defaultValue = "ext4")
    String fsType;

    /**
     * Wether the volumeName is read only or not.
     */
    @ConfigItem
    boolean readOnly;

}
