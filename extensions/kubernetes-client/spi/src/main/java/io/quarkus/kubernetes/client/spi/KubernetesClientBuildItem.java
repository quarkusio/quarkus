package io.quarkus.kubernetes.client.spi;

import java.util.function.Supplier;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.quarkus.builder.item.SimpleBuildItem;

public final class KubernetesClientBuildItem extends SimpleBuildItem {

    private final Config config;

    public KubernetesClientBuildItem(Config config) {
        this.config = config;
    }

    public Supplier<KubernetesClient> getClient() {
        return () -> new KubernetesClientBuilder().withConfig(config).build();
    }

    public Config getConfig() {
        return config;
    }

}
