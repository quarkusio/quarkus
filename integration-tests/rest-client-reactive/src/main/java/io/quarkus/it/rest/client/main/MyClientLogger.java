package io.quarkus.it.rest.client.main;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.resteasy.reactive.client.api.ClientLogger;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;

@ApplicationScoped
public class MyClientLogger implements ClientLogger {
    private final AtomicBoolean used = new AtomicBoolean(false);

    @Override
    public void setBodySize(int bodySize) {
    }

    @Override
    public void logResponse(HttpClientResponse response, boolean redirect) {
        used.set(true);
    }

    @Override
    public void logRequest(HttpClientRequest request, Buffer body, boolean omitBody) {
        used.set(true);
    }

    public void reset() {
        used.set(false);
    }

    public boolean wasUsed() {
        return used.get();
    }
}
