package io.quarkus.kubernetes.config.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "kubernetes-config", phase = ConfigPhase.BUILD_TIME)
public class KubernetesConfigBuildTimeConfig {
    /**
     * Whether or not configuration can be read from secrets.
     * If set to {@code true}, Kubernetes resources allowing access to secrets (role and role binding) will be generated.
     */
    @ConfigItem(name = "secrets.enabled", defaultValue = "false")
    public boolean secretsEnabled;
}
