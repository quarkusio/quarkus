package io.quarkus.kubernetes.config.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.kubernetes-config")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface KubernetesConfigSourceConfig {

    /**
     * If set to true, the application will attempt to look up the configuration from the API server
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * If set to true, the application will not start if any of the configured config sources cannot be located
     */
    @WithDefault("true")
    boolean failOnMissingConfig();

    /**
     * ConfigMaps to look for in the namespace that the Kubernetes Client has been configured for. ConfigMaps defined
     * later in this list have a higher priority that ConfigMaps defined earlier in this list. Furthermore, any Secrets
     * defined in {@code secrets}, will have higher priorities than all ConfigMaps.
     */
    Optional<List<String>> configMaps();

    /**
     * Secrets to look for in the namespace that the Kubernetes Client has been configured for. If you use this, you
     * probably want to enable {@code quarkus.kubernetes-config.secrets.enabled}. Secrets defined later in this list
     * have a higher priority that ConfigMaps defined earlier in this list. Furthermore, these Secrets have a higher
     * priorities than all ConfigMaps defined in {@code configMaps}.
     */
    Optional<List<String>> secrets();

    /**
     * Namespace to look for config maps and secrets. If this is not specified, then the namespace configured in the
     * kubectl config context is used. If the value is specified and the namespace doesn't exist, the application will
     * fail to start.
     */
    Optional<String> namespace();
}
