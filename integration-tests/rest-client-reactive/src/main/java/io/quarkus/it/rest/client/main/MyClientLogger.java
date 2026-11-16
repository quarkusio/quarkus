package io.quarkus.it.rest.client.main;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.resteasy.reactive.client.api.ClientLogger;
import org.jboss.resteasy.reactive.client.logging.DefaultClientLogger;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;

@ApplicationScoped
public class MyClientLogger implements ClientLogger {
    private final AtomicInteger used = new AtomicInteger(0);

    private final ClientLogger delegate = new DefaultClientLogger();

    @Override
    public void setBodySize(int bodySize) {
        delegate.setBodySize(bodySize);
    }

    @Override
    public void setMaskedHeaders(Set<String> maskedHeaders) {
        delegate.setMaskedHeaders(maskedHeaders);
    }

    @Override
    public void logResponse(HttpClientResponse response, boolean redirect) {
        used.incrementAndGet();
        delegate.logResponse(response, redirect);
    }

    @Override
    public void logResponse(HttpClientResponse response, Buffer body, boolean redirect) {
        delegate.logResponse(response, body, redirect);
        used.incrementAndGet();
    }

    @Override
    public void logRequest(HttpClientRequest request, Buffer body, boolean omitBody) {
        delegate.logRequest(request, body, omitBody);
        used.incrementAndGet();
    }

    public void reset() {
        used.set(0);
    }

    public boolean wasUsed() {
        return used.get() > 0;
    }

    public int timesUsed() {
        return used.get();
    }
}
