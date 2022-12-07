package io.quarkus.opentelemetry.runtime.tracing.intrumentation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.EventBusInstrumenterVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.HttpInstrumenterVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.InstrumenterVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.OpenTelemetryVertxMetricsFactory;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.OpenTelemetryVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.OpenTelemetryVertxTracingDevModeFactory;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.OpenTelemetryVertxTracingFactory;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.SqlClientInstrumenterVertxTracer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.VertxOptions;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.tracing.TracingOptions;

@Recorder
public class InstrumentationRecorder {

    public static final OpenTelemetryVertxTracingDevModeFactory FACTORY = new OpenTelemetryVertxTracingDevModeFactory();

    /* RUNTIME INIT */
    public RuntimeValue<OpenTelemetryVertxTracer> createTracers() {
        OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
        List<InstrumenterVertxTracer<?, ?>> instrumenterVertxTracers = new ArrayList<>();
        instrumenterVertxTracers.add(new HttpInstrumenterVertxTracer(openTelemetry));
        instrumenterVertxTracers.add(new EventBusInstrumenterVertxTracer(openTelemetry));
        // TODO - Selectively register this in the recorder if the SQL Client is available.
        instrumenterVertxTracers.add(new SqlClientInstrumenterVertxTracer(openTelemetry));
        return new RuntimeValue<>(new OpenTelemetryVertxTracer(instrumenterVertxTracers));
    }

    /* RUNTIME INIT */
    public Consumer<VertxOptions> getVertxTracingOptionsProd(
            RuntimeValue<OpenTelemetryVertxTracer> tracer) {
        TracingOptions tracingOptions = new TracingOptions()
                .setFactory(new OpenTelemetryVertxTracingFactory(tracer.getValue()));
        return vertxOptions -> vertxOptions.setTracingOptions(tracingOptions);
    }

    /* RUNTIME INIT */
    public Consumer<VertxOptions> getVertxTracingOptionsDevMode() {
        TracingOptions tracingOptions = new TracingOptions()
                .setFactory(FACTORY);
        return vertxOptions -> vertxOptions.setTracingOptions(tracingOptions);
    }

    /* RUNTIME INIT */
    public void setTracerDevMode(RuntimeValue<OpenTelemetryVertxTracer> tracer) {
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
