package io.quarkus.kubernetes.deployment;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.smallrye.config.WithDefault;

public interface AzureDiskVolumeConfig {
    enum CachingMode {
        ReadWrite,
        ReadOnly,
        None
    }

    enum Kind {
        Managed,
        Shared
    }

    /**
     * The name of the disk to mount.
     */
    String diskName();

    /**
     * The URI of the vhd blob object OR the resourceID of an Azure managed data disk if Kind is Managed
     */
    String diskURI();

    /**
     * Kind of disk.
     */
    @WithDefault("Managed")
    Kind kind();

    /**
     * Disk caching mode.
     */
    @WithDefault("ReadWrite")
    CachingMode cachingMode();

    /**
     * File system type.
     */
    @WithDefault("ext4")
    String fsType();

    /**
     * Whether the volume is read only or not.
     */
    @WithDefault("false")
    boolean readOnly();

    default Volume toVolume(String name) {
        return new VolumeBuilder()
                .withName(name)
                .withNewAzureDisk()
                .withKind(kind().name())
                .withDiskName(diskName())
                .withDiskURI(diskURI())
                .withFsType(fsType())
                .withCachingMode(cachingMode().name())
                .withReadOnly(readOnly())
                .endAzureDisk()
                .build();
    }
}
