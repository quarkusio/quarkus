
package io.quarkus.kubernetes.deployment;

import java.util.Map;

import io.dekorate.kubernetes.config.ConfigMapVolume;
import io.dekorate.kubernetes.config.ConfigMapVolumeBuilder;

public class ConfigMapVolumeConverter {

    public static ConfigMapVolume convert(Map.Entry<String, ConfigMapVolumeConfig> e) {
        return convert(e.getValue()).withVolumeName(e.getKey()).build();
    }

    public static ConfigMapVolumeBuilder convert(ConfigMapVolumeConfig cm) {
        ConfigMapVolumeBuilder b = new ConfigMapVolumeBuilder();
        b.withConfigMapName(cm.configMapName);
        b.withDefaultMode(FilePermissionUtil.parseInt(cm.defaultMode));
        b.withOptional(cm.optional);
        return b;
    }

}
