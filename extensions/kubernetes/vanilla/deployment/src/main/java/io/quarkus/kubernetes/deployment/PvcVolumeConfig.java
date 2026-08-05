package io.quarkus.kubernetes.deployment;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.smallrye.config.WithDefault;

public interface PvcVolumeConfig {
    /**
     * The name of the claim to mount.
     */
    String claimName();

    /**
     * Default mode. When specifying an octal number, leading zero must be present.
     *
     * @deprecated This actually doesn't make sense for PVC and should not be used
     */
    @WithDefault("0600")
    @Deprecated(forRemoval = true)
    String defaultMode();

    /**
     * Optional
     *
     * @deprecated Use {@link #readOnly()} instead
     */
    @WithDefault("false")
    @Deprecated(forRemoval = true)
    boolean optional();

    /**
     * Whether the associated PVC is read-only.
     */
    @WithDefault("false")
    boolean readOnly();

    default Volume toVolume(String name) {
        return new VolumeBuilder()
                .withName(name)
                .withNewPersistentVolumeClaim()
                .withClaimName(claimName())
                // since default is false, this should cover using the deprecated optional correctly
                .withReadOnly(readOnly() || optional())
                .endPersistentVolumeClaim()
                .build();
    }
}
