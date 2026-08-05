package io.quarkus.kubernetes.deployment;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.smallrye.config.WithDefault;

public interface AzureFileVolumeConfig {
    /**
     * The share name.
     */
    String shareName();

    /**
     * The secret name.
     */
    String secretName();

    /**
     * Whether the volume is read only or not.
     */
    @WithDefault("false")
    boolean readOnly();

    default Volume toVolume(String name) {
        return new VolumeBuilder()
                .withName(name)
                .withNewAzureFile()
                .withSecretName(secretName())
                .withShareName(shareName())
                .withReadOnly(readOnly())
                .endAzureFile()
                .build();
    }
}
