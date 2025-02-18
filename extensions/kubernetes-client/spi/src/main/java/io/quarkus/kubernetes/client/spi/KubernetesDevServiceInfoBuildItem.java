package io.quarkus.kubernetes.client.spi;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.builder.item.SimpleBuildItem;

public final class KubernetesDevServiceInfoBuildItem extends SimpleBuildItem {
    private final String kubeConfig;
    private final GenericContainer container;

    public KubernetesDevServiceInfoBuildItem(String kubeConfig, GenericContainer container) {
        this.kubeConfig = kubeConfig;
        this.container = container;
    }

    public String getKubeConfig() {
        return kubeConfig;
    }

    public GenericContainer getContainer() {
        return container;
    }

}
