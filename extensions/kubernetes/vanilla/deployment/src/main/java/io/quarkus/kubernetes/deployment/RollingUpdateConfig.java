package io.quarkus.kubernetes.deployment;

import io.smallrye.config.WithDefault;

public interface RollingUpdateConfig {
    /**
     * Specifies the maximum number of Pods that can be unavailable during the update process.
     */
    @WithDefault("25%")
    String maxUnavailable();

    /**
     * Specifies the maximum number of Pods that can be created over the desired number of Pods.
     */
    @WithDefault("25%")
    String maxSurge();
}
