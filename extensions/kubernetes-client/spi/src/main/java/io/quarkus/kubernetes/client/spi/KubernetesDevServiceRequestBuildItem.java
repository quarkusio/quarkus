package io.quarkus.kubernetes.client.spi;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * BuildItem managing the Kubernetes DevService Request information for the extension consuming it
 */
public final class KubernetesDevServiceRequestBuildItem extends SimpleBuildItem {
    private final String flavor;

    public KubernetesDevServiceRequestBuildItem(String flavor) {
        this.flavor = flavor;
    }

    /**
     * @return the flavor of the kubernetes cluster to start: kind, k3s, etc
     */
    public String getFlavor() {
        return flavor;
    }
}
