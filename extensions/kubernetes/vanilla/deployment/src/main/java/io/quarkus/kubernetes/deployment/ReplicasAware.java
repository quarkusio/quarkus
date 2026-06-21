package io.quarkus.kubernetes.deployment;

import io.smallrye.config.WithDefault;

public interface ReplicasAware {
    /**
     * The number of desired pods
     */
    @WithDefault("1")
    Integer replicas();
}
