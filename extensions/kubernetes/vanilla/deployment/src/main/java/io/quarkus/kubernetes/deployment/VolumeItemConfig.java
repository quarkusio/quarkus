package io.quarkus.kubernetes.deployment;

import io.smallrye.config.WithDefault;

public interface VolumeItemConfig {
    /**
     * The path where the file will be mounted.
     */
    String path();

    /**
     * It must be a value between 0000 and 0777. If not specified, the volume defaultMode will be used.
     */
    @WithDefault("-1")
    int mode();

    /**
     * @return {@code true} if {@link #mode()} was explicitly configured
     */
    default boolean hasExplicitMode() {
        return mode() >= 0;
    }
}
