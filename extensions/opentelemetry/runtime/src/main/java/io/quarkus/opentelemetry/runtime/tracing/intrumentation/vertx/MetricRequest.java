package io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx;

import java.util.Optional;

import io.vertx.core.Context;
import io.vertx.core.http.impl.HttpServerRequestInternal;
import io.vertx.core.spi.observability.HttpRequest;

public final class MetricRequest {
    private final HttpRequest request;

    MetricRequest(final HttpRequest request) {
        this.request = request;
    }

    Optional<Context> getContext() {
        if (request instanceof HttpServerRequestInternal) {
            return Optional.of(((HttpServerRequestInternal) request).context());
        } else {
            return Optional.empty();
        }
    }

    static MetricRequest request(final HttpRequest httpRequest) {
        return new MetricRequest(httpRequest);
    }
}
