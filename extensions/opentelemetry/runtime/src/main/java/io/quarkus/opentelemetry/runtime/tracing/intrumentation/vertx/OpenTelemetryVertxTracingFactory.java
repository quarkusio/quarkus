package io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx;

import io.vertx.core.spi.VertxTracerFactory;
import io.vertx.core.spi.tracing.VertxTracer;
import io.vertx.core.tracing.TracingOptions;

public class OpenTelemetryVertxTracingFactory implements VertxTracerFactory {
    private final OpenTelemetryVertxTracer openTelemetryVertxTracer;

    public OpenTelemetryVertxTracingFactory(OpenTelemetryVertxTracer openTelemetryVertxTracer) {
        this.openTelemetryVertxTracer = openTelemetryVertxTracer;
    }

    @Override
    public VertxTracer<?, ?> tracer(final TracingOptions options) {
        return openTelemetryVertxTracer;
    }
}
