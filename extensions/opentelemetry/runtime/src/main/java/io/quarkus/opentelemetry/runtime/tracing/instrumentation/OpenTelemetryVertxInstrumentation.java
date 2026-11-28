package io.quarkus.opentelemetry.runtime.tracing.instrumentation;

import java.util.ArrayList;
import java.util.List;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.EventBusInstrumenterVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.GrpcHttpInstrumenterVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.HttpInstrumenterVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.InstrumenterVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.OpenTelemetryVertxHttpMetricsFactory;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.OpenTelemetryVertxMetricsFactory;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.OpenTelemetryVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.OpenTelemetryVertxTracingFactory;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.RedisClientInstrumenterVertxTracer;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.SqlClientInstrumenterVertxTracer;

/**
 * Instruments Vert.x: the tracer factory set on the Vert.x bootstrap, the tracers it delegates to, and the metrics factories.
 */
public final class OpenTelemetryVertxInstrumentation {

    public static final OpenTelemetryVertxTracingFactory FACTORY = new OpenTelemetryVertxTracingFactory();

    private OpenTelemetryVertxInstrumentation() {
    }

    public static OTelVertxBootstrapCustomizer tracing(final OTelRuntimeConfig runtimeConfig) {
        if (runtimeConfig.sdkDisabled()) {
            return bootstrap -> {
            };
        }
        return bootstrap -> bootstrap.tracerFactory(FACTORY);
    }

    public static void setupVertxTracer(BeanContainer beanContainer, OTelBuildConfig buildConfig,
            OTelRuntimeConfig runtimeConfig, boolean sqlClientAvailable, boolean redisClientAvailable,
            boolean grpcAvailable) {
        OpenTelemetry openTelemetry = beanContainer.beanInstance(OpenTelemetry.class); // always force initialization of OTel

        if (runtimeConfig.sdkDisabled()) {
            return;
        }

        List<InstrumenterVertxTracer<?, ?>> tracers = new ArrayList<>(5);
        if (grpcAvailable && buildConfig.instrument().grpc()
                && runtimeConfig.instrument().vertxHttp()) {
            tracers.add(new GrpcHttpInstrumenterVertxTracer(openTelemetry));
        }
        if (runtimeConfig.instrument().vertxHttp()) {
            tracers.add(new HttpInstrumenterVertxTracer(openTelemetry, runtimeConfig, buildConfig));
        }
        if (runtimeConfig.instrument().vertxEventBus()) {
            tracers.add(new EventBusInstrumenterVertxTracer(openTelemetry, runtimeConfig));
        }
        if (sqlClientAvailable && runtimeConfig.instrument().vertxSqlClient()) {
            tracers.add(new SqlClientInstrumenterVertxTracer(openTelemetry, runtimeConfig));
        }
        if (redisClientAvailable && runtimeConfig.instrument().vertxRedisClient()) {
            tracers.add(new RedisClientInstrumenterVertxTracer(openTelemetry, runtimeConfig));
        }
        OpenTelemetryVertxTracer openTelemetryVertxTracer = new OpenTelemetryVertxTracer(tracers);
        FACTORY.getVertxTracerDelegator().setDelegate(openTelemetryVertxTracer);
    }

    public static OTelVertxBootstrapCustomizer httpMetrics() {
        return bootstrap -> bootstrap.metricsFactory(new OpenTelemetryVertxHttpMetricsFactory());
    }

    public static OTelVertxBootstrapCustomizer metrics() {
        return bootstrap -> bootstrap.metricsFactory(new OpenTelemetryVertxMetricsFactory());
    }
}
