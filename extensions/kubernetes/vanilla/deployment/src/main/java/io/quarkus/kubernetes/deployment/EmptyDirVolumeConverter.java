
package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.config.EmptyDirVolume;
import io.dekorate.kubernetes.config.EmptyDirVolumeBuilder;

public class EmptyDirVolumeConverter {

    public static EmptyDirVolume convert(String volumeName) {
        EmptyDirVolumeBuilder b = new EmptyDirVolumeBuilder();
        b.withVolumeName(volumeName);
        return b.build();
    }

}
