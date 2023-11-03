package io.quarkus.kubernetes.client.spi;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.quarkus.builder.item.SimpleBuildItem;

public final class KubernetesClientBuildItem extends SimpleBuildItem {

    private final Config config;
    private final HttpClient.Factory httpClientFactory;

    public KubernetesClientBuildItem(Config config, HttpClient.Factory httpClientFactory) {
        this.config = config;
        this.httpClientFactory = httpClientFactory;
    }

    public Config getConfig() {
        return config;
    }

    public HttpClient.Factory getHttpClientFactory() {
        return httpClientFactory;
    }

    public KubernetesClient buildClient() {
        return new KubernetesClientBuilder().withConfig(config).withHttpClientFactory(httpClientFactory).build();
    }
}
