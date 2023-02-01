package io.quarkus.kubernetes.client.runtime;

import javax.enterprise.inject.spi.CDI;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.vertx.VertxHttpClientBuilder;
import io.vertx.core.Vertx;

public class QuarkusHttpClientFactory implements io.fabric8.kubernetes.client.http.HttpClient.Factory {

    private Vertx vertx;

    public QuarkusHttpClientFactory() {
        // The client might get initialized outside a Quarkus context that can provide the Vert.x instance
        // This is the case for the MockServer / @KubernetesTestServer where the server provides the KubernetesClient instance
        try {
            this.vertx = CDI.current().select(Vertx.class).get();
        } catch (Exception e) {
            this.vertx = Vertx.vertx();
        }
    }

    @Override
    public HttpClient.Builder newBuilder(Config config) {
        return HttpClient.Factory.super.newBuilder(config);
    }

    @Override
    public VertxHttpClientBuilder<QuarkusHttpClientFactory> newBuilder() {
        return new VertxHttpClientBuilder<>(this, vertx);
    }

    @Override
    public int priority() {
        return 1;
    }
}
