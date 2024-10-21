package io.quarkus.kubernetes.deployment;

import java.util.OptionalInt;

import io.smallrye.config.WithDefault;

public interface AwsElasticBlockStoreVolumeConfig {
    /**
     * The name of the disk to mount.
     */
    String volumeId();

    /**
     * The partition.
     */
    OptionalInt partition();

    /**
     * Filesystem type.
     */
    @WithDefault("ext4")
    String fsType();

    /**
     * Whether the volumeName is read only or not.
     */
    @WithDefault("false")
    boolean readOnly();
}
