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
import io.vertx.core.internal.VertxBootstrap;

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
    public Consumer<VertxBootstrap> processVertxBootstrap() {
        if (runtimeConfig.getValue().sdkDisabled()) {
            return bootstrap -> {
            };
        }
        return bootstrap -> bootstrap.tracerFactory(FACTORY);
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
    public Consumer<VertxBootstrap> getVertxHttpMetrics() {
        return bootstrap -> bootstrap.metricsFactory(new OpenTelemetryVertxHttpMetricsFactory());
    }

    /* STATIC INIT */
    public Consumer<VertxBootstrap> getVertxMetricsOptions() {
        return bootstrap -> bootstrap.metricsFactory(new OpenTelemetryVertxMetricsFactory());
    }

}
