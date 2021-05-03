package io.quarkus.opentelemetry.tracing.vertx;

import io.vertx.core.tracing.TracingOptions;

public class VertxOpenTelemetryOptions extends TracingOptions {
    public VertxOpenTelemetryOptions() {
        setFactory(new VertxOpenTelemetryTracingFactory());
    }
}
