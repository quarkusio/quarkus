package io.quarkus.opentelemetry.runtime.tracing.vertx;

import java.util.ArrayList;
import java.util.List;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.vertx.core.spi.VertxTracerFactory;
import io.vertx.core.spi.tracing.VertxTracer;
import io.vertx.core.tracing.TracingOptions;

public class OpenTelemetryVertxTracingFactory implements VertxTracerFactory {
    @Override
    public VertxTracer<?, ?> tracer(final TracingOptions options) {
        OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
        List<InstrumenterVertxTracer<?, ?>> instrumenterVertxTracers = new ArrayList<>();
        instrumenterVertxTracers.add(new HttpInstrumenterVertxTracer(openTelemetry));
        instrumenterVertxTracers.add(new EventBusInstrumenterVertxTracer(openTelemetry));
        // TODO - Selectively register this in the recorder if the SQL Client is available.
        instrumenterVertxTracers.add(new SqlClientInstrumenterVertxTracer(openTelemetry));
        return new OpenTelemetryVertxTracer(instrumenterVertxTracers);
    }
}
