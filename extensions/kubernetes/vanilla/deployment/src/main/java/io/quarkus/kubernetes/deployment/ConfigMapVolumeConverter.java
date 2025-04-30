
package io.quarkus.kubernetes.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.dekorate.kubernetes.config.ConfigMapVolume;
import io.dekorate.kubernetes.config.ConfigMapVolumeBuilder;
import io.dekorate.kubernetes.config.Item;
import io.dekorate.kubernetes.config.ItemBuilder;

public class ConfigMapVolumeConverter {

    public static ConfigMapVolume convert(Map.Entry<String, ConfigMapVolumeConfig> e) {
        return convert(e.getValue()).withVolumeName(e.getKey()).build();
    }

    public static ConfigMapVolumeBuilder convert(ConfigMapVolumeConfig cm) {
        ConfigMapVolumeBuilder b = new ConfigMapVolumeBuilder();
        b.withConfigMapName(cm.configMapName());
        b.withDefaultMode(FilePermissionUtil.parseInt(cm.defaultMode()));
        b.withOptional(cm.optional());
        if (cm.items() != null && !cm.items().isEmpty()) {
            List<Item> items = new ArrayList<>(cm.items().size());
            for (Map.Entry<String, VolumeItemConfig> item : cm.items().entrySet()) {
                items.add(new ItemBuilder()
                        .withKey(item.getKey())
                        .withPath(item.getValue().path())
                        .withMode(item.getValue().mode())
                        .build());
            }

            b.withItems(items.toArray(new Item[items.size()]));
        }
        return b;
    }

}
