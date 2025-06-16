package io.quarkus.opentelemetry.runtime.metrics.instrumentation;

import static io.opentelemetry.instrumentation.runtimemetrics.java17.JfrFeature.CLASS_LOAD_METRICS;
import static io.opentelemetry.instrumentation.runtimemetrics.java17.JfrFeature.CONTEXT_SWITCH_METRICS;
import static io.opentelemetry.instrumentation.runtimemetrics.java17.JfrFeature.CPU_COUNT_METRICS;
import static io.opentelemetry.instrumentation.runtimemetrics.java17.JfrFeature.CPU_UTILIZATION_METRICS;
import static io.opentelemetry.instrumentation.runtimemetrics.java17.JfrFeature.GC_DURATION_METRICS;
import static io.opentelemetry.instrumentation.runtimemetrics.java17.JfrFeature.LOCK_METRICS;
import static io.opentelemetry.instrumentation.runtimemetrics.java17.JfrFeature.MEMORY_ALLOCATION_METRICS;
import static io.opentelemetry.instrumentation.runtimemetrics.java17.JfrFeature.MEMORY_POOL_METRICS;
import static io.opentelemetry.instrumentation.runtimemetrics.java17.JfrFeature.NETWORK_IO_METRICS;
import static io.opentelemetry.instrumentation.runtimemetrics.java17.JfrFeature.THREAD_METRICS;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.runtimemetrics.java17.RuntimeMetrics;
import io.opentelemetry.instrumentation.runtimemetrics.java17.RuntimeMetricsBuilder;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.runtime.ImageMode;
import io.quarkus.runtime.Startup;

@Startup
@ApplicationScoped
public class JvmMetricsService {

    private final RuntimeMetrics runtimeMetrics;

    public JvmMetricsService(final OpenTelemetry openTelemetry, final OTelRuntimeConfig runtimeConfig) {

        if (runtimeConfig.sdkDisabled() || !runtimeConfig.instrument().jvmMetrics()) {
            runtimeMetrics = RuntimeMetrics.builder(openTelemetry).disableAllMetrics().build();
            return;
        }

        RuntimeMetricsBuilder builder = RuntimeMetrics.builder(openTelemetry)
                .enableFeature(CONTEXT_SWITCH_METRICS)
                .enableFeature(CPU_COUNT_METRICS)
                .enableFeature(LOCK_METRICS)
                .enableFeature(NETWORK_IO_METRICS)
                .disableFeature(MEMORY_POOL_METRICS);

        if (ImageMode.current().isNativeImage()) {
            builder.enableFeature(THREAD_METRICS);
            builder.enableFeature(CLASS_LOAD_METRICS);
            builder.enableFeature(GC_DURATION_METRICS);
            builder.enableFeature(CPU_UTILIZATION_METRICS);
            builder.enableFeature(MEMORY_ALLOCATION_METRICS);
        } else {
            builder.disableFeature(THREAD_METRICS);
            builder.disableFeature(CLASS_LOAD_METRICS);
            builder.disableFeature(GC_DURATION_METRICS);
            builder.disableFeature(CPU_UTILIZATION_METRICS);
            builder.disableFeature(MEMORY_ALLOCATION_METRICS);
        }

        runtimeMetrics = builder.build();
    }

    @PreDestroy
    public void close() {
        if (runtimeMetrics != null) {
            runtimeMetrics.close();
        }
    }

}
