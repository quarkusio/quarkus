package io.quarkus.kubernetes.config.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.kubernetes-config")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface KubernetesConfigBuildTimeConfig {
    /**
     * Whether configuration can be read from secrets. If set to {@code true}, Kubernetes resources allowing access to
     * secrets (role and role binding) will be generated.
     */
    @WithName("secrets.enabled")
    @WithDefault("false")
    boolean secretsEnabled();

    /**
     * Role configuration to generate if the "secrets-enabled" property is true.
     */
    SecretsRoleConfig secretsRoleConfig();
}
