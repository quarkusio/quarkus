package io.quarkus.kubernetes.deployment;

import java.util.Map;

import io.dekorate.kubernetes.config.PersistentVolumeClaimVolume;
import io.dekorate.kubernetes.config.PersistentVolumeClaimVolumeBuilder;

public class PvcVolumeConverter {

    public static PersistentVolumeClaimVolume convert(Map.Entry<String, PvcVolumeConfig> e) {
        return convert(e.getValue()).withVolumeName(e.getKey()).build();
    }

    public static PersistentVolumeClaimVolumeBuilder convert(PvcVolumeConfig c) {
        PersistentVolumeClaimVolumeBuilder b = new PersistentVolumeClaimVolumeBuilder();
        b.withClaimName(c.claimName());
        b.withReadOnly(c.optional());
        return b;
    }
}
