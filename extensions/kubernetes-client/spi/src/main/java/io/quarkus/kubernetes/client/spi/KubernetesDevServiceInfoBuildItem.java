package io.quarkus.kubernetes.client.spi;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * BuildItem packaging the information of the kind test container created
 */
public final class KubernetesDevServiceInfoBuildItem extends SimpleBuildItem {
    private final String kubeConfig;
    private final String containerId;

    public KubernetesDevServiceInfoBuildItem(String kubeConfig, String containerId) {
        this.kubeConfig = kubeConfig;
        this.containerId = containerId;
    }

    /**
     * @return the KubeConfig as YAML string
     */
    public String getKubeConfig() {
        return kubeConfig;
    }

    /**
     * @return the containerId of the running kind test container
     */
    public String getContainerId() {
        return containerId;
    }

}