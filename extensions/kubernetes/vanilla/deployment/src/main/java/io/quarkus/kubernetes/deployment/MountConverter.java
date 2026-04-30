
package io.quarkus.kubernetes.deployment;

import java.util.Map;

import io.dekorate.kubernetes.config.Mount;
import io.dekorate.kubernetes.config.MountBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;

public class MountConverter {

    public static Mount convert(Map.Entry<String, MountConfig> e) {
        return convert(e.getValue()).withName(e.getKey()).build();
    }

    public static VolumeMount toVolumeMount(Map.Entry<String, MountConfig> e) {
        VolumeMountBuilder b = new VolumeMountBuilder().withName(e.getKey());
        final var mount = e.getValue();
        mount.path().ifPresent(b::withMountPath);
        mount.subPath().ifPresent(b::withSubPath);
        b.withReadOnly(mount.readOnly());
        return b.build();
    }

    private static MountBuilder convert(MountConfig mount) {
        MountBuilder b = new MountBuilder();
        mount.path().ifPresent(b::withPath);
        mount.subPath().ifPresent(b::withSubPath);
        b.withReadOnly(mount.readOnly());
        return b;
    }
}
