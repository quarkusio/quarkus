package io.quarkus.rest.client.reactive.runtime.context;

import jakarta.ws.rs.ext.ContextResolver;

import io.vertx.core.http.HttpClientOptions;

public class HttpClientOptionsContextResolver implements ContextResolver<HttpClientOptions> {

    private final HttpClientOptions component;

    public HttpClientOptionsContextResolver(HttpClientOptions component) {
        this.component = component;
    }

    @Override
    public HttpClientOptions getContext(Class<?> wantedClass) {
        if (wantedClass.equals(HttpClientOptions.class)) {
            return component;
        }

        return null;
    }
}
