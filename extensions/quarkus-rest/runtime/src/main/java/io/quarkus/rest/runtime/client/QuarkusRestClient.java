package io.quarkus.rest.runtime.client;

import java.net.URI;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;

import io.quarkus.rest.runtime.core.Serialisers;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestConfiguration;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;

public class QuarkusRestClient implements Client {

    final Vertx vertx = VertxCoreRecorder.getVertx().get();
    final HttpClient httpClient;
    final QuarkusRestConfiguration configuration = new QuarkusRestConfiguration();
    final Serialisers serialisers;
    final ClientProxies clientProxies;
    final HostnameVerifier hostnameVerifier;
    final SSLContext sslContext;

    public QuarkusRestClient(Serialisers serialisers, ClientProxies clientProxies, HostnameVerifier hostnameVerifier,
            SSLContext sslContext) {
        this.serialisers = serialisers;
        this.clientProxies = clientProxies;
        this.hostnameVerifier = hostnameVerifier;
        this.sslContext = sslContext;
        this.httpClient = vertx.createHttpClient();
    }

    @Override
    public void close() {
        httpClient.close();
    }

    @Override
    public WebTarget target(String uri) {
        return new QuarkusRestWebTarget(httpClient, UriBuilder.fromUri(uri), configuration, serialisers, clientProxies);
    }

    @Override
    public WebTarget target(URI uri) {
        return new QuarkusRestWebTarget(httpClient, UriBuilder.fromUri(uri), configuration, serialisers, clientProxies);
    }

    @Override
    public WebTarget target(UriBuilder uriBuilder) {
        return new QuarkusRestWebTarget(httpClient, uriBuilder, configuration, serialisers, clientProxies);
    }

    @Override
    public WebTarget target(Link link) {
        return new QuarkusRestWebTarget(httpClient, UriBuilder.fromLink(link), configuration, serialisers, clientProxies);
    }

    @Override
    public Invocation.Builder invocation(Link link) {
        return target(link).request();
    }

    @Override
    public SSLContext getSslContext() {
        return sslContext;
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public Client property(String name, Object value) {
        configuration.property(name, value);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass) {
        configuration.register(componentClass);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass, int priority) {
        configuration.register(componentClass, priority);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass, Class<?>... contracts) {
        configuration.register(componentClass, contracts);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        configuration.register(componentClass, contracts);
        return this;
    }

    @Override
    public Client register(Object component) {
        configuration.register(component);
        return this;
    }

    @Override
    public Client register(Object component, int priority) {
        configuration.register(component, priority);
        return this;
    }

    @Override
    public Client register(Object component, Class<?>... contracts) {
        configuration.register(component, contracts);
        return this;
    }

    @Override
    public Client register(Object component, Map<Class<?>, Integer> contracts) {
        configuration.register(component, contracts);
        return this;
    }
}
