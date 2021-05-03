package io.quarkus.opentelemetry.tracing.vertx;

import io.opentelemetry.api.trace.Span;
import io.vertx.core.spi.VertxTracerFactory;
import io.vertx.core.spi.tracing.VertxTracer;
import io.vertx.core.tracing.TracingOptions;

public class VertxOpenTelemetryTracingFactory implements VertxTracerFactory {
    @Override
    public VertxTracer<Span, Span> tracer(final TracingOptions options) {
        return new VertxOpenTelemetryTracer();
    }
}
