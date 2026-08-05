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
     * The configuration of the `wait for` container.
     */
    InitTaskContainerConfig waitForContainer();

    /**
     * Configuration of the Role and RoleBinding generated so the wait-for init container can
     * {@code get} Job resources.
     * <p>
     * Only honored when set on {@code quarkus.kubernetes.init-task-defaults.rbac}
     * (or the OpenShift equivalent). Per-task {@code init-tasks.*.rbac} values are ignored
     * because a single Role is shared by all init tasks.
     */
    InitTaskRbacConfig rbac();

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

    interface InitTaskRbacConfig {
        /**
         * The name of the Role.
         */
        @WithDefault("view-jobs")
        String name();

        /**
         * If the Role is meant to be generated. When {@code false}, only the RoleBinding is
         * generated (pointing at an existing Role with the resolved name).
         */
        @WithDefault("true")
        boolean generate();

        /**
         * If set to true, the Role name is prefixed with the application name, producing a unique
         * name per application (e.g. {@code my-app-view-jobs}). This prevents name conflicts when
         * multiple applications sharing the same namespace each manage their own Role.
         */
        @WithDefault("true")
        boolean prefixName();
    }
}
