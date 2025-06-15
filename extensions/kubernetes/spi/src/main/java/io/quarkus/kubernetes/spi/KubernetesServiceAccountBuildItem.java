package io.quarkus.kubernetes.spi;

import java.util.Collections;
import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Produce this build item to request the Kubernetes extension to generate a Kubernetes {@code ServiceAccount} resource.
 */
public final class KubernetesServiceAccountBuildItem extends MultiBuildItem {
    /**
     * Name of the generated {@code ServiceAccount} resource.
     */
    private final String name;
    /**
     * Namespace of the generated {@code ServiceAccount} resource.
     */
    private final String namespace;
    /**
     * Labels of the generated {@code ServiceAccount} resource.
     */
    private final Map<String, String> labels;

    /**
     * If true, this service account will be used in the generated Deployment resources.
     */
    private final boolean useAsDefault;

    /**
     * With empty parameters, it will generate a service account with the same name that the deployment.
     */
    public KubernetesServiceAccountBuildItem(boolean useAsDefault) {
        this(null, null, Collections.emptyMap(), useAsDefault);
    }

    public KubernetesServiceAccountBuildItem(String name, String namespace, Map<String, String> labels,
            boolean useAsDefault) {
        this.name = name;
        this.namespace = namespace;
        this.labels = labels;
        this.useAsDefault = useAsDefault;
    }

    public String getName() {
        return this.name;
    }

    public String getNamespace() {
        return namespace;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public boolean isUseAsDefault() {
        return useAsDefault;
    }
}
