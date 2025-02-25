package io.quarkus.kubernetes.service.binding.deployment;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.kubernetes-service-binding")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface KubernetesServiceBindingConfig {

    /**
     * A list of explicitly configured services.
     * The configured value will be used in order to generate the `ServiceBinding` resource (in case that kubernetes resource
     * generation is enabled).
     */
    Map<String, ServiceConfig> services();

    /**
     * The mountPath to add in the `ServiceBinding` resource.
     */
    Optional<String> mountPath();

    /**
     * Determines if binding should be created as files or env vars.
     * Set this value to {@code false} to bind as env vars.`
     */
    @WithDefault("true")
    Boolean bindAsFiles();

    /**
     * Detects the binding data from resources owned by the backing service.
     */
    @WithDefault("false")
    Boolean detectBindingResources();
}
