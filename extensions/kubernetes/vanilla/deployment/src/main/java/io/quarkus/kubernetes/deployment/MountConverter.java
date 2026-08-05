
package io.quarkus.kubernetes.deployment;

import java.util.Map;

import io.dekorate.kubernetes.config.Mount;
import io.dekorate.kubernetes.config.MountBuilder;

public class MountConverter {

    public static Mount convert(Map.Entry<String, MountConfig> e) {
        return convert(e.getValue()).withName(e.getKey()).build();
    }

    private static MountBuilder convert(MountConfig mount) {
        MountBuilder b = new MountBuilder();
        mount.path().ifPresent(b::withPath);
        mount.subPath().ifPresent(b::withSubPath);
        b.withReadOnly(mount.readOnly());
        return b;
    }
}
