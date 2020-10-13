package io.quarkus.rest.runtime.client;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

import io.quarkus.rest.runtime.client.handlers.ClientErrorHandler;
import io.quarkus.rest.runtime.client.handlers.ClientRequestFiltersRestHandler;
import io.quarkus.rest.runtime.client.handlers.ClientResponseRestHandler;
import io.quarkus.rest.runtime.client.handlers.ClientSendRequestHandler;
import io.quarkus.rest.runtime.core.GenericTypeMapping;
import io.quarkus.rest.runtime.core.Serialisers;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestConfiguration;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;

public class QuarkusRestClient implements Client {

    final Vertx vertx;
    final boolean closeVertx;
    final HttpClient httpClient;
    final QuarkusRestConfiguration configuration;
    final Serialisers serialisers;
    final ClientProxies clientProxies;
    final GenericTypeMapping genericTypeMapping;
    final HostnameVerifier hostnameVerifier;
    final SSLContext sslContext;
    private boolean isClosed;
    final ClientRestHandler[] handlerChain;
    final ClientRestHandler[] abortHandlerChain;

    public QuarkusRestClient(QuarkusRestConfiguration configuration, Serialisers serialisers, ClientProxies clientProxies,
            GenericTypeMapping genericTypeMapping, HostnameVerifier hostnameVerifier,
            SSLContext sslContext, Supplier<Vertx> vertx) {
        this.configuration = configuration != null ? configuration : new QuarkusRestConfiguration(RuntimeType.CLIENT);
        this.serialisers = serialisers;
        this.clientProxies = clientProxies;
        this.genericTypeMapping = genericTypeMapping;
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
        abortHandlerChain = new ClientRestHandler[] { new ClientErrorHandler() };
        handlerChain = new ClientRestHandler[] { new ClientRequestFiltersRestHandler(), new ClientSendRequestHandler(),
                new ClientResponseRestHandler() };
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
        // close is checked in the other target call
        Objects.requireNonNull(uri);
        return target(UriBuilder.fromUri(uri));
    }

    @Override
    public WebTarget target(URI uri) {
        // close is checked in the other target call
        Objects.requireNonNull(uri);
        return target(UriBuilder.fromUri(uri));
    }

    @Override
    public WebTarget target(UriBuilder uriBuilder) {
        abortIfClosed();
        Objects.requireNonNull(uriBuilder);
        return new QuarkusRestWebTarget(this, httpClient, uriBuilder, new QuarkusRestConfiguration(configuration), serialisers,
                clientProxies, genericTypeMapping, handlerChain, abortHandlerChain, null);
    }

    @Override
    public WebTarget target(Link link) {
        // close is checked in the other target call
        Objects.requireNonNull(link);
        return target(UriBuilder.fromLink(link));
    }

    @Override
    public Invocation.Builder invocation(Link link) {
        abortIfClosed();
        Objects.requireNonNull(link);
        Builder request = target(link).request();
        if (link.getType() != null)
            request.accept(link.getType());
        return request;
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
    public QuarkusRestConfiguration getConfiguration() {
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

    Vertx getVertx() {
        return vertx;
    }
}
