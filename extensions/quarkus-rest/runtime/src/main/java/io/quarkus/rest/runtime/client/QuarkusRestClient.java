package io.quarkus.rest.runtime.client;

import java.net.URI;
import java.util.Map;
import java.util.function.Supplier;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

import io.quarkus.rest.runtime.core.Serialisers;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestConfiguration;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;

public class QuarkusRestClient implements Client {

    final Vertx vertx;
    final boolean closeVertx;
    final HttpClient httpClient;
    final QuarkusRestConfiguration configuration = new QuarkusRestConfiguration(RuntimeType.CLIENT);
    final Serialisers serialisers;
    final ClientProxies clientProxies;
    final HostnameVerifier hostnameVerifier;
    final SSLContext sslContext;
    private boolean isClosed;

    public QuarkusRestClient(Serialisers serialisers, ClientProxies clientProxies, HostnameVerifier hostnameVerifier,
            SSLContext sslContext, Supplier<Vertx> vertx) {
        this.serialisers = serialisers;
        this.clientProxies = clientProxies;
        this.hostnameVerifier = hostnameVerifier;
        this.sslContext = sslContext;
        if (vertx != null) {
            this.vertx = vertx.get();
            closeVertx = false;
        } else {
            this.vertx = Vertx.vertx();
            closeVertx = true;
        }
        this.httpClient = this.vertx.createHttpClient();
    }

    @Override
    public void close() {
        if (isClosed)
            return;
        isClosed = true;
        httpClient.close();
        if (closeVertx) {
            vertx.close();
        }
    }

    void abortIfClosed() {
        if (isClosed)
            throw new IllegalStateException("Client is closed");
    }

    @Override
    public WebTarget target(String uri) {
        abortIfClosed();
        return new QuarkusRestWebTarget(this, httpClient, UriBuilder.fromUri(uri), new QuarkusRestConfiguration(configuration),
                serialisers, clientProxies);
    }

    @Override
    public WebTarget target(URI uri) {
        abortIfClosed();
        return new QuarkusRestWebTarget(this, httpClient, UriBuilder.fromUri(uri), new QuarkusRestConfiguration(configuration),
                serialisers, clientProxies);
    }

    @Override
    public WebTarget target(UriBuilder uriBuilder) {
        abortIfClosed();
        return new QuarkusRestWebTarget(this, httpClient, uriBuilder, new QuarkusRestConfiguration(configuration), serialisers,
                clientProxies);
    }

    @Override
    public WebTarget target(Link link) {
        abortIfClosed();
        return new QuarkusRestWebTarget(this, httpClient, UriBuilder.fromLink(link),
                new QuarkusRestConfiguration(configuration),
                serialisers, clientProxies);
    }

    @Override
    public Invocation.Builder invocation(Link link) {
        abortIfClosed();
        return target(link).request();
    }

    @Override
    public SSLContext getSslContext() {
        abortIfClosed();
        return sslContext;
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        abortIfClosed();
        return hostnameVerifier;
    }

    @Override
    public Configuration getConfiguration() {
        abortIfClosed();
        return configuration;
    }

    @Override
    public Client property(String name, Object value) {
        abortIfClosed();
        configuration.property(name, value);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass) {
        abortIfClosed();
        configuration.register(componentClass);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass, int priority) {
        abortIfClosed();
        configuration.register(componentClass, priority);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass, Class<?>... contracts) {
        abortIfClosed();
        configuration.register(componentClass, contracts);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        abortIfClosed();
        configuration.register(componentClass, contracts);
        return this;
    }

    @Override
    public Client register(Object component) {
        abortIfClosed();
        configuration.register(component);
        return this;
    }

    @Override
    public Client register(Object component, int priority) {
        abortIfClosed();
        configuration.register(component, priority);
        return this;
    }

    @Override
    public Client register(Object component, Class<?>... contracts) {
        abortIfClosed();
        configuration.register(component, contracts);
        return this;
    }

    @Override
    public Client register(Object component, Map<Class<?>, Integer> contracts) {
        abortIfClosed();
        configuration.register(component, contracts);
        return this;
    }
}
