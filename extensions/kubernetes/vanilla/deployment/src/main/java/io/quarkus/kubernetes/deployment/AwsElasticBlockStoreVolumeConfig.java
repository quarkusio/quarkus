package io.quarkus.kubernetes.deployment;

import java.util.OptionalInt;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
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
     * Whether the volume is read only or not.
     */
    @WithDefault("false")
    boolean readOnly();

    default Volume toVolume(String name) {
        final var awsStore = new VolumeBuilder()
                .withName(name)
                .withNewAwsElasticBlockStore();

        partition().ifPresent(awsStore::withPartition);

        return awsStore
                .withVolumeID(volumeId())
                .withFsType(fsType())
                .withReadOnly(readOnly())
                .endAwsElasticBlockStore()
                .build();
    }
}
