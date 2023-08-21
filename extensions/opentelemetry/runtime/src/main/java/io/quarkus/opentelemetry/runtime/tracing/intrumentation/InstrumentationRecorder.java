package io.quarkus.opentelemetry.runtime.tracing.intrumentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.SpanConfig;
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

    private final RuntimeValue<OTelRuntimeConfig> config;

    public InstrumentationRecorder(RuntimeValue<OTelRuntimeConfig> config) {
        this.config = config;
    }

    /* RUNTIME INIT */
    public Consumer<VertxOptions> getVertxTracingOptions() {
        TracingOptions tracingOptions = new TracingOptions()
                .setFactory(FACTORY);
        return vertxOptions -> vertxOptions.setTracingOptions(tracingOptions);
    }

    /* RUNTIME INIT */
    public void setupVertxTracer(BeanContainer beanContainer) {
        OpenTelemetry openTelemetry = beanContainer.beanInstance(OpenTelemetry.class);
        List<SpanConfig.Tracers> enabledTracers = config.getValue().span().enabledTracers();
        List<InstrumenterVertxTracer<?, ?>> vertxTracers = enabledTracers.isEmpty() ? Collections.emptyList()
                : new ArrayList<>(enabledTracers.size());
        if (enabledTracers.contains(SpanConfig.Tracers.HTTP)) {
            vertxTracers.add(new HttpInstrumenterVertxTracer(openTelemetry));
        }
        if (enabledTracers.contains(SpanConfig.Tracers.EVENTBUS)) {
            vertxTracers.add(new EventBusInstrumenterVertxTracer(openTelemetry));
        }
        // TODO - Selectively register this in the recorder if the SQL Client is available.
        if (enabledTracers.contains(SpanConfig.Tracers.SQL)) {
            vertxTracers.add(new SqlClientInstrumenterVertxTracer(openTelemetry));
        }
        OpenTelemetryVertxTracer openTelemetryVertxTracer = new OpenTelemetryVertxTracer(vertxTracers);
        FACTORY.getVertxTracerDelegator().setDelegate(openTelemetryVertxTracer);
    }

    /* RUNTIME INIT */
    public Consumer<VertxOptions> getVertxTracingMetricsOptions() {
        MetricsOptions metricsOptions = new MetricsOptions()
                .setEnabled(true)
                .setFactory(new OpenTelemetryVertxMetricsFactory());
        return vertxOptions -> vertxOptions.setMetricsOptions(metricsOptions);
    }

}
