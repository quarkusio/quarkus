package io.quarkus.kubernetes.service.binding.buildtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "kubernetes-service-binding", phase = ConfigPhase.BUILD_TIME)
public class KubernetesServiceBindingConfig {

    /**
     * A list of explicitly configured services.
     * The configured value will be used in order to generate the `ServiceBinding` resource (in case that kubernetes resource
     * generation is enabled).
     */
    @ConfigItem
    public Map<String, ServiceConfig> services;

    /**
     * The mountPath to add in the `ServiceBinding` resource.
     */
    @ConfigItem
    public Optional<String> mountPath;

    /**
     * Determines if binding should be created as files or env vars.
     * Set this value to {@code false} to bind as env vars.`
     */
    @ConfigItem(defaultValue = "true")
    public Boolean bindAsFiles;

    /**
     * Detects the binding data from resources owned by the backing service.
     */
    @ConfigItem(defaultValue = "false")
    public Boolean detectBindingResources;
}
