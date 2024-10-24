package io.quarkus.kubernetes.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.dekorate.kubernetes.config.Item;
import io.dekorate.kubernetes.config.ItemBuilder;
import io.dekorate.kubernetes.config.SecretVolume;
import io.dekorate.kubernetes.config.SecretVolumeBuilder;

public class SecretVolumeConverter {

    public static SecretVolume convert(Map.Entry<String, SecretVolumeConfig> e) {
        return convert(e.getValue()).withVolumeName(e.getKey()).build();
    }

    public static SecretVolumeBuilder convert(SecretVolumeConfig c) {
        SecretVolumeBuilder b = new SecretVolumeBuilder();
        b.withSecretName(c.secretName());
        b.withDefaultMode(FilePermissionUtil.parseInt(c.defaultMode()));
        b.withOptional(c.optional());
        if (c.items() != null && !c.items().isEmpty()) {
            List<Item> items = new ArrayList<>(c.items().size());
            for (Map.Entry<String, VolumeItemConfig> item : c.items().entrySet()) {
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
