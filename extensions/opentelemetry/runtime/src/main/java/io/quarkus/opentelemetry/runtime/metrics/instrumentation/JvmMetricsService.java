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
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
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

        // Will only produce mandatory metrics for MP Telemetry 2.0
        runtimeMetrics = RuntimeMetrics.builder(openTelemetry)
                .disableFeature(CONTEXT_SWITCH_METRICS)
                .disableFeature(CPU_COUNT_METRICS)
                .disableFeature(LOCK_METRICS)
                .disableFeature(MEMORY_ALLOCATION_METRICS)
                .disableFeature(NETWORK_IO_METRICS)
                .enableFeature(MEMORY_POOL_METRICS)
                .enableFeature(GC_DURATION_METRICS)
                .enableFeature(THREAD_METRICS)
                .enableFeature(CLASS_LOAD_METRICS)
                .enableFeature(CPU_UTILIZATION_METRICS)
                .build();
    }

    @PreDestroy
    public void close() {
        if (runtimeMetrics != null) {
            runtimeMetrics.close();
        }
    }

}
