package io.quarkus.kubernetes.client.runtime;

import static io.vertx.core.spi.resolver.ResolverProvider.DISABLE_DNS_RESOLVER_PROP_NAME;

import jakarta.enterprise.inject.spi.CDI;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.vertx.VertxHttpClientBuilder;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class QuarkusHttpClientFactory implements io.fabric8.kubernetes.client.http.HttpClient.Factory {

    private Vertx vertx;

    public QuarkusHttpClientFactory() {
        // The client might get initialized outside a Quarkus context that can provide the Vert.x instance
        // This is the case for the MockServer / @KubernetesTestServer where the server provides the KubernetesClient instance
        try {
            this.vertx = CDI.current().select(Vertx.class).get();
        } catch (Exception e) {
            this.vertx = createVertxInstance();
        }
    }

    private Vertx createVertxInstance() {
        // We must disable the async DNS resolver as it can cause issues when resolving the Vault instance.
        // This is done using the DISABLE_DNS_RESOLVER_PROP_NAME system property.
        // The DNS resolver used by vert.x is configured during the (synchronous) initialization.
        // So, we just need to disable the async resolver around the Vert.x instance creation.
        String originalValue = System.getProperty(DISABLE_DNS_RESOLVER_PROP_NAME);
        Vertx vertx;
        try {
            System.setProperty(DISABLE_DNS_RESOLVER_PROP_NAME, "true");
            vertx = Vertx.vertx(new VertxOptions());
        } finally {
            // Restore the original value
            if (originalValue == null) {
                System.clearProperty(DISABLE_DNS_RESOLVER_PROP_NAME);
            } else {
                System.setProperty(DISABLE_DNS_RESOLVER_PROP_NAME, originalValue);
            }
        }
        return vertx;
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
