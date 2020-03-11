package io.quarkus.kubernetes.deployment;

import java.util.Map;

import io.dekorate.kubernetes.config.SecretVolume;
import io.dekorate.kubernetes.config.SecretVolumeBuilder;

public class SecretVolumeConverter {

    public static SecretVolume convert(Map.Entry<String, SecretVolumeConfig> e) {
        return convert(e.getValue()).withVolumeName(e.getKey()).build();
    }

    public static SecretVolumeBuilder convert(SecretVolumeConfig c) {
        SecretVolumeBuilder b = new SecretVolumeBuilder();
        b.withSecretName(c.secretName);
        b.withDefaultMode(FilePermissionUtil.parseInt(c.defaultMode));
        b.withOptional(c.optional);
        return b;
    }
}
