package io.quarkus.kubernetes.deployment;

import java.util.Map;

import io.dekorate.kubernetes.config.AzureDiskVolume;
import io.dekorate.kubernetes.config.AzureDiskVolumeBuilder;

public class AzureDiskVolumeConverter {

    public static AzureDiskVolume convert(Map.Entry<String, AzureDiskVolumeConfig> e) {
        return convert(e.getValue()).withVolumeName(e.getKey()).build();
    }

    private static AzureDiskVolumeBuilder convert(AzureDiskVolumeConfig c) {
        AzureDiskVolumeBuilder b = new AzureDiskVolumeBuilder();
        b.withDiskName(c.diskName());
        b.withDiskURI(c.diskURI());
        b.withKind(c.kind().name());
        b.withCachingMode(c.cachingMode().name());
        b.withFsType(c.fsType());
        b.withReadOnly(c.readOnly());
        return b;
    }
}
