package io.quarkus.kubernetes.client.runtime.internal;

import static io.vertx.core.spi.resolver.ResolverProvider.DISABLE_DNS_RESOLVER_PROP_NAME;

import java.io.Closeable;

import jakarta.enterprise.inject.spi.CDI;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.vertx.VertxHttpClientBuilder;
import io.quarkus.runtime.ResettableSystemProperties;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.file.FileSystemOptions;

public class QuarkusHttpClientFactory implements HttpClient.Factory, Closeable {

    private Vertx vertx;
    private boolean closeVertxOnExit;

    public QuarkusHttpClientFactory() {
        // The client might get initialized outside a Quarkus context that can provide the Vert.x instance
        // This is the case for the MockServer / @KubernetesTestServer where the server provides the KubernetesClient instance
        try {
            this.vertx = CDI.current().select(Vertx.class).get();
            this.closeVertxOnExit = false;
        } catch (Exception e) {
            this.vertx = createVertxInstance();
            this.closeVertxOnExit = true;
        }
    }

    private Vertx createVertxInstance() {
        // We must disable the async DNS resolver as it can cause issues when resolving the Vault instance.
        // This is done using the DISABLE_DNS_RESOLVER_PROP_NAME system property.
        // The DNS resolver used by vert.x is configured during the (synchronous) initialization.
        // So, we just need to disable the async resolver around the Vert.x instance creation.
        try (var resettableSystemProperties = ResettableSystemProperties.of(
                DISABLE_DNS_RESOLVER_PROP_NAME, "true")) {
            return Vertx.vertx(new VertxOptions().setFileSystemOptions(
                    new FileSystemOptions().setFileCachingEnabled(false).setClassPathResolvingEnabled(false)));

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

    @Override
    public void close() {
        if (closeVertxOnExit) {
            vertx.close();
        }
    }
}
