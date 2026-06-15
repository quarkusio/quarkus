package io.quarkus.kubernetes.deployment;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;

public interface ConfigMapVolumeConfig extends VolumeConfig {
    /**
     * The name of the ConfigMap to mount.
     */
    String configMapName();

    default Volume toVolume(String name) {
        final var volume = new VolumeBuilder()
                .withName(name)
                .withNewConfigMap();
        items().forEach((k, v) -> {
            final var item = volume.addNewItem().withKey(k).withPath(v.path());
            if (v.hasExplicitMode()) {
                item.withMode(v.mode());
            }
            item.endItem();
        });

        return volume
                .withName(configMapName())
                .withDefaultMode(FilePermissionUtil.parseInt(defaultMode()))
                .withOptional(optional())
                .endConfigMap()
                .build();
    }
}
