package io.quarkus.kubernetes.client.spi;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * BuildItem recording information of the started Kubernetes test container
 */
public final class KubernetesDevServiceInfoBuildItem extends SimpleBuildItem {
    private String kubeConfig;
    private final ConfigurationSupplier configurationSupplier;

    public KubernetesDevServiceInfoBuildItem(ConfigurationSupplier configurationSupplier) {
        this.configurationSupplier = configurationSupplier;
    }

    /**
     * @return the KubeConfig as YAML string
     */
    public String getKubeConfig() {
        if (kubeConfig == null) {
            kubeConfig = configurationSupplier.getKubeConfig();
        }
        return kubeConfig;
    }

    /**
     * @return the containerId of the running test container
     */
    @SuppressWarnings("unused")
    public String getContainerId() {
        return configurationSupplier.getContainerId();
    }

    public interface ConfigurationSupplier {
        String getKubeConfig();

        String getContainerId();
    }
}
