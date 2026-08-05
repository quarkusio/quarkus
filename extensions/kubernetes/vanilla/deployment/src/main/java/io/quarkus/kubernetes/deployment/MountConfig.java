package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.smallrye.config.WithDefault;

public interface MountConfig {
    /**
     * The name of the volumeName to mount.
     */
    Optional<String> name();

    /**
     * The path to mount.
     */
    Optional<String> path();

    /**
     * Path within the volumeName from which the container's volumeName should be mounted.
     */
    Optional<String> subPath();

    /**
     * ReadOnly.
     */
    @WithDefault("false")
    boolean readOnly();

    default VolumeMount toVolumeMount(String name) {
        VolumeMountBuilder b = new VolumeMountBuilder().withName(name);
        path().ifPresent(b::withMountPath);
        subPath().ifPresent(b::withSubPath);
        b.withReadOnly(readOnly());
        return b.build();
    }
}
