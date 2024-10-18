
package io.quarkus.kubernetes.deployment;

import java.util.Map;

import io.dekorate.kubernetes.config.AzureFileVolume;
import io.dekorate.kubernetes.config.AzureFileVolumeBuilder;

public class AzureFileVolumeConverter {

    public static AzureFileVolume convert(Map.Entry<String, AzureFileVolumeConfig> e) {
        return convert(e.getValue()).withVolumeName(e.getKey()).build();
    }

    private static AzureFileVolumeBuilder convert(AzureFileVolumeConfig c) {
        AzureFileVolumeBuilder b = new AzureFileVolumeBuilder();
        b.withSecretName(c.secretName());
        b.withShareName(c.shareName());
        b.withReadOnly(c.readOnly());
        return b;
    }
}
