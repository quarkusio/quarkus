package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.dekorate.kubernetes.annotation.ImagePullPolicy;
import io.smallrye.config.WithDefault;

public interface InitTaskConfig {
    /**
     * If true, the init task will be generated. Otherwise, the init task resource generation will be skipped.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The init task image to use by the init container.
     *
     * @deprecated use waitForContainer.image instead.
     */
    @Deprecated(forRemoval = true, since = "3.5")
    Optional<String> image();

    /**
     * Limits the lifetime of an init-task Job that has finished execution (either Complete or Failed).
     * If set, after this many seconds the Job is eligible to be automatically deleted by Kubernetes.
     */
    Optional<Integer> ttlSecondsAfterFinished();

    /**
     * The configuration of the `wait for` container.
     */
    InitTaskContainerConfig waitForContainer();

    interface InitTaskContainerConfig {
        /**
         * The init task image to use by the init container.
         */
        @WithDefault("groundnuty/k8s-wait-for:no-root-v1.7")
        String image();

        /**
         * Image pull policy.
         */
        @WithDefault("always")
        ImagePullPolicy imagePullPolicy();
    }
}
