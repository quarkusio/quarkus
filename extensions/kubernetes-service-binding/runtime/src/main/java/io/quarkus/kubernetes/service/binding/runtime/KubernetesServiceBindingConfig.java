package io.quarkus.kubernetes.service.binding.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "kubernetes-service-binding", phase = ConfigPhase.BOOTSTRAP)
public class KubernetesServiceBindingConfig {

    /**
     * If enabled, Service Bindings will be looked in the file system
     */
    @ConfigItem
    public boolean enabled;

    /**
     * The bindings file system root. Specified by the Kubernetes Service ServiceBinding Specification.
     */
    @ConfigItem(defaultValue = "${SERVICE_BINDING_ROOT:}")
    public Optional<String> root;
}
