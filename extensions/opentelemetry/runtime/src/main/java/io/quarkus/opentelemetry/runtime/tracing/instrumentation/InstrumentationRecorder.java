package io.quarkus.opentelemetry.runtime.tracing.instrumentation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.EventBusInstrumenterVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.HttpInstrumenterVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.InstrumenterVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.OpenTelemetryVertxHttpMetricsFactory;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.OpenTelemetryVertxMetricsFactory;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.OpenTelemetryVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.OpenTelemetryVertxTracingFactory;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.RedisClientInstrumenterVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.SqlClientInstrumenterVertxTracer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.annotations.RuntimeInit;
import io.vertx.core.VertxOptions;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.tracing.TracingOptions;

@Recorder
public class InstrumentationRecorder {

    public static final OpenTelemetryVertxTracingFactory FACTORY = new OpenTelemetryVertxTracingFactory();

    private final OTelBuildConfig buildConfig;
    private final RuntimeValue<OTelRuntimeConfig> runtimeConfig;

    public InstrumentationRecorder(
            final OTelBuildConfig buildConfig,
            final RuntimeValue<OTelRuntimeConfig> runtimeConfig) {
        this.buildConfig = buildConfig;
        this.runtimeConfig = runtimeConfig;
    }

    /* RUNTIME INIT */
    @RuntimeInit
    public Consumer<VertxOptions> getVertxTracingOptions() {
        if (runtimeConfig.getValue().sdkDisabled()) {
            return vertxOptions -> {
            };
        }
        TracingOptions tracingOptions = new TracingOptions()
                .setFactory(FACTORY);
        return vertxOptions -> vertxOptions.setTracingOptions(tracingOptions);
    }

    /* RUNTIME INIT */
    @RuntimeInit
    public void setupVertxTracer(BeanContainer beanContainer, boolean sqlClientAvailable, boolean redisClientAvailable) {
        OpenTelemetry openTelemetry = beanContainer.beanInstance(OpenTelemetry.class); // always force initialization of OTel

        if (runtimeConfig.getValue().sdkDisabled()) {
            return;
        }

        List<InstrumenterVertxTracer<?, ?>> tracers = new ArrayList<>(4);
        if (runtimeConfig.getValue().instrument().vertxHttp()) {
            tracers.add(new HttpInstrumenterVertxTracer(openTelemetry, runtimeConfig.getValue(), buildConfig));
        }
        if (runtimeConfig.getValue().instrument().vertxEventBus()) {
            tracers.add(new EventBusInstrumenterVertxTracer(openTelemetry, runtimeConfig.getValue()));
        }
        if (sqlClientAvailable && runtimeConfig.getValue().instrument().vertxSqlClient()) {
            tracers.add(new SqlClientInstrumenterVertxTracer(openTelemetry, runtimeConfig.getValue()));
        }
        if (redisClientAvailable && runtimeConfig.getValue().instrument().vertxRedisClient()) {
            tracers.add(new RedisClientInstrumenterVertxTracer(openTelemetry, runtimeConfig.getValue()));
        }
        OpenTelemetryVertxTracer openTelemetryVertxTracer = new OpenTelemetryVertxTracer(tracers);
        FACTORY.getVertxTracerDelegator().setDelegate(openTelemetryVertxTracer);
    }

    /* STATIC INIT */
    public Consumer<VertxOptions> getVertxHttpMetricsOptions() {
        MetricsOptions metricsOptions = new MetricsOptions()
                .setEnabled(true)
                .setFactory(new OpenTelemetryVertxHttpMetricsFactory());
        return vertxOptions -> vertxOptions.setMetricsOptions(metricsOptions);
    }

    /* STATIC INIT */
    public Consumer<VertxOptions> getVertxMetricsOptions() {
        MetricsOptions metricsOptions = new MetricsOptions()
                .setEnabled(true)
                .setFactory(new OpenTelemetryVertxMetricsFactory());
        return vertxOptions -> vertxOptions.setMetricsOptions(metricsOptions);
    }

}
