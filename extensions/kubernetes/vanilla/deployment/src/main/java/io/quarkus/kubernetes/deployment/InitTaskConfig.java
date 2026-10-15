package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.annotation.ImagePullPolicy;
import io.smallrye.config.WithDefault;

public interface InitTaskConfig {
    /**
     * If true, the init task will be generated. Otherwise, the init task resource generation will be skipped.
     */
    @WithDefault("true")
    boolean enabled();

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
         * The name of the Role. When unset, defaults to {@code {application-name}-view-jobs}
         * so multiple applications in the same namespace each get a unique Role. Set this to the
         * exact Role name you want (no application-name prefix is applied).
         */
        Optional<String> name();

        /**
         * If the Role is meant to be generated. When {@code false}, only the RoleBinding is
         * generated (pointing at an existing Role with the resolved name).
         */
        @WithDefault("true")
        boolean generate();
    }
}
