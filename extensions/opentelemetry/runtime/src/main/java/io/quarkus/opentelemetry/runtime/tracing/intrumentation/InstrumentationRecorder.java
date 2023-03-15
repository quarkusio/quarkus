package io.quarkus.opentelemetry.runtime.tracing.intrumentation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.EventBusInstrumenterVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.HttpInstrumenterVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.InstrumenterVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.OpenTelemetryVertxMetricsFactory;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.OpenTelemetryVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.OpenTelemetryVertxTracingFactory;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.SqlClientInstrumenterVertxTracer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.VertxOptions;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.tracing.TracingOptions;

@Recorder
public class InstrumentationRecorder {

    public static final OpenTelemetryVertxTracingFactory FACTORY = new OpenTelemetryVertxTracingFactory();

    /* RUNTIME INIT */
    public RuntimeValue<OpenTelemetryVertxTracer> createTracers(RuntimeValue<OpenTelemetry> openTelemetry) {
        List<InstrumenterVertxTracer<?, ?>> instrumenterVertxTracers = new ArrayList<>();
        instrumenterVertxTracers.add(new HttpInstrumenterVertxTracer(openTelemetry.getValue()));
        instrumenterVertxTracers.add(new EventBusInstrumenterVertxTracer(openTelemetry.getValue()));
        // TODO - Selectively register this in the recorder if the SQL Client is available.
        instrumenterVertxTracers.add(new SqlClientInstrumenterVertxTracer(openTelemetry.getValue()));
        return new RuntimeValue<>(new OpenTelemetryVertxTracer(instrumenterVertxTracers));
    }

    /* RUNTIME INIT */
    public Consumer<VertxOptions> getVertxTracingOptions() {
        TracingOptions tracingOptions = new TracingOptions()
                .setFactory(FACTORY);
        return vertxOptions -> vertxOptions.setTracingOptions(tracingOptions);
    }

    /* RUNTIME INIT */
    public void setTracer(RuntimeValue<OpenTelemetryVertxTracer> tracer) {
        FACTORY.getVertxTracerDelegator().setDelegate(tracer.getValue());
    }

    /* RUNTIME INIT */
    public Consumer<VertxOptions> getVertxTracingMetricsOptions() {
        MetricsOptions metricsOptions = new MetricsOptions()
                .setEnabled(true)
                .setFactory(new OpenTelemetryVertxMetricsFactory());
        return vertxOptions -> vertxOptions.setMetricsOptions(metricsOptions);
    }
}
