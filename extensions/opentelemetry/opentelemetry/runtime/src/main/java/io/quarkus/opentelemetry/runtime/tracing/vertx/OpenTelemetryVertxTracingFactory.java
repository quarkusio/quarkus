package io.quarkus.opentelemetry.runtime.tracing.vertx;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.vertx.core.spi.VertxTracerFactory;
import io.vertx.core.spi.tracing.VertxTracer;
import io.vertx.core.tracing.TracingOptions;

public class OpenTelemetryVertxTracingFactory implements VertxTracerFactory {
    @Override
    public VertxTracer tracer(final TracingOptions options) {
        return new OpenTelemetryVertxTracer(GlobalOpenTelemetry.get());
    }
}
