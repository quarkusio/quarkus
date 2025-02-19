package io.quarkus.kubernetes.client.spi;

import io.quarkus.builder.item.SimpleBuildItem;

public final class KubernetesDevServiceInfoBuildItem extends SimpleBuildItem {
    private final String kubeConfig;
    private final String containerId;

    public KubernetesDevServiceInfoBuildItem(String kubeConfig, String containerId) {
        this.kubeConfig = kubeConfig;
        this.containerId = containerId;
    }

    public String getKubeConfig() {
        return kubeConfig;
    }

    public String getContainerId() {
        return containerId;
    }

}