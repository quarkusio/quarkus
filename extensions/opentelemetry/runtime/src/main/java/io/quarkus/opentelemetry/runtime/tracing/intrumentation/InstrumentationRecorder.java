package io.quarkus.opentelemetry.runtime.tracing.intrumentation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.EventBusInstrumenterVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.HttpInstrumenterVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.InstrumenterVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.OpenTelemetryVertxMetricsFactory;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.OpenTelemetryVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.OpenTelemetryVertxTracingFactory;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.RedisClientInstrumenterVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.SqlClientInstrumenterVertxTracer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.annotations.RuntimeInit;
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

    @RuntimeInit
    public Consumer<VertxOptions> getVertxTracingOptions() {
        TracingOptions tracingOptions = new TracingOptions()
                .setFactory(FACTORY);
        return vertxOptions -> vertxOptions.setTracingOptions(tracingOptions);
    }

    /* RUNTIME INIT */
    @RuntimeInit
    public void setupVertxTracer(BeanContainer beanContainer, boolean sqlClientAvailable,
            boolean redisClientAvailable) {
        OpenTelemetry openTelemetry = beanContainer.beanInstance(OpenTelemetry.class);
        List<InstrumenterVertxTracer<?, ?>> tracers = new ArrayList<>(4);
        if (config.getValue().instrument().vertxHttp()) {
            tracers.add(new HttpInstrumenterVertxTracer(openTelemetry));
        }
        if (config.getValue().instrument().vertxEventBus()) {
            tracers.add(new EventBusInstrumenterVertxTracer(openTelemetry));
        }
        if (sqlClientAvailable && config.getValue().instrument().vertxSqlClient()) {
            tracers.add(new SqlClientInstrumenterVertxTracer(openTelemetry));
        }
        if (redisClientAvailable && config.getValue().instrument().vertxRedisClient()) {
            tracers.add(new RedisClientInstrumenterVertxTracer(openTelemetry));
        }
        OpenTelemetryVertxTracer openTelemetryVertxTracer = new OpenTelemetryVertxTracer(tracers);
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
