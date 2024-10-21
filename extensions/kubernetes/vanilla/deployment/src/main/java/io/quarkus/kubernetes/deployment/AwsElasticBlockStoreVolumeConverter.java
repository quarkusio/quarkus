
package io.quarkus.kubernetes.deployment;

import java.util.Map;

import io.dekorate.kubernetes.config.AwsElasticBlockStoreVolume;
import io.dekorate.kubernetes.config.AwsElasticBlockStoreVolumeBuilder;

public class AwsElasticBlockStoreVolumeConverter {

    public static AwsElasticBlockStoreVolume convert(Map.Entry<String, AwsElasticBlockStoreVolumeConfig> e) {
        return convert(e.getValue()).withVolumeName(e.getKey()).build();
    }

    private static AwsElasticBlockStoreVolumeBuilder convert(AwsElasticBlockStoreVolumeConfig c) {
        AwsElasticBlockStoreVolumeBuilder b = new AwsElasticBlockStoreVolumeBuilder();
        b.withVolumeId(c.volumeId());
        b.withFsType(c.fsType());
        b.withReadOnly(c.readOnly());
        c.partition().ifPresent(p -> b.withPartition(p));
        return b;
    }
}
