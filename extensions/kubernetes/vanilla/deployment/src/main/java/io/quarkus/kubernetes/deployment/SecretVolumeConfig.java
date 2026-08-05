package io.quarkus.kubernetes.deployment;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;

public interface SecretVolumeConfig extends VolumeConfig {
    /**
     * The name of the secret to mount.
     */
    String secretName();

    default Volume toVolume(String name) {
        final var volume = new VolumeBuilder()
                .withName(name)
                .withNewSecret();
        items().forEach((k, v) -> {
            final var item = volume.addNewItem().withKey(k).withPath(v.path());
            if (v.hasExplicitMode()) {
                item.withMode(v.mode());
            }
            item.endItem();
        });

        return volume
                .withSecretName(secretName())
                .withDefaultMode(FilePermissionUtil.parseInt(defaultMode()))
                .withOptional(optional())
                .endSecret()
                .build();
    }
}
