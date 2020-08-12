package io.quarkus.qrs.runtime.client;

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

import io.quarkus.qrs.runtime.core.Serialisers;
import io.quarkus.qrs.runtime.jaxrs.QrsConfiguration;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;

public class QrsClient implements Client {

    final Vertx vertx = VertxCoreRecorder.getVertx().get();
    final HttpClient httpClient;
    final QrsConfiguration configuration = new QrsConfiguration();
    final Serialisers serialisers;

    public QrsClient(Serialisers serialisers) {
        this.serialisers = serialisers;
        this.httpClient = vertx.createHttpClient();
    }

    @Override
    public void close() {

    }

    @Override
    public WebTarget target(String uri) {
        return new QrsWebTarget(httpClient, UriBuilder.fromUri(uri), configuration, serialisers);
    }

    @Override
    public WebTarget target(URI uri) {
        return new QrsWebTarget(httpClient, UriBuilder.fromUri(uri), configuration, serialisers);
    }

    @Override
    public WebTarget target(UriBuilder uriBuilder) {
        return new QrsWebTarget(httpClient, uriBuilder, configuration, serialisers);
    }

    @Override
    public WebTarget target(Link link) {
        return new QrsWebTarget(httpClient, UriBuilder.fromLink(link), configuration, serialisers);
    }

    @Override
    public Invocation.Builder invocation(Link link) {
        return null;
    }

    @Override
    public SSLContext getSslContext() {
        return null;
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        return null;
    }

    @Override
    public Configuration getConfiguration() {
        return null;
    }

    @Override
    public Client property(String name, Object value) {
        return null;
    }

    @Override
    public Client register(Class<?> componentClass) {
        return null;
    }

    @Override
    public Client register(Class<?> componentClass, int priority) {
        return null;
    }

    @Override
    public Client register(Class<?> componentClass, Class<?>... contracts) {
        return null;
    }

    @Override
    public Client register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        return null;
    }

    @Override
    public Client register(Object component) {
        return null;
    }

    @Override
    public Client register(Object component, int priority) {
        return null;
    }

    @Override
    public Client register(Object component, Class<?>... contracts) {
        return null;
    }

    @Override
    public Client register(Object component, Map<Class<?>, Integer> contracts) {
        return null;
    }
}
