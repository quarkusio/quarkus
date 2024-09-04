package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class KubernetesNamespaceBuildItem extends MultiBuildItem {

    private final String target; // Kubernetes, Openshift, Knative
    private final String namespace;

    public KubernetesNamespaceBuildItem(String target, String namespace) {
        this.target = target;
        this.namespace = namespace;
    }

    public String getTarget() {
        return target;
    }

    public String getNamespace() {
        return namespace;
    }
}
