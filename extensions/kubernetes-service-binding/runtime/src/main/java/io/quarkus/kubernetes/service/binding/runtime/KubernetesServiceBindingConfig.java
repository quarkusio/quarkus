package io.quarkus.kubernetes.service.binding.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.kubernetes-service-binding")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@Deprecated(forRemoval = true, since = "3.19")
public interface KubernetesServiceBindingConfig {

    /**
     * If enabled, Service Bindings will be looked in the file system
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The bindings file system root. Specified by the Kubernetes Service ServiceBinding Specification.
     */
    @WithDefault("${SERVICE_BINDING_ROOT:}")
    Optional<String> root();
}
