package io.quarkus.opentelemetry.runtime.metrics.instrumentation;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.runtimetelemetry.RuntimeTelemetry;
import io.opentelemetry.instrumentation.runtimetelemetry.RuntimeTelemetryBuilder;
import io.opentelemetry.instrumentation.runtimetelemetry.internal.Internal;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.runtime.ImageMode;
import io.quarkus.runtime.Startup;

@Startup
@ApplicationScoped
public class JvmMetricsService {

    private final RuntimeTelemetry runtimeTelemetry;

    public JvmMetricsService(final OpenTelemetry openTelemetry, final OTelRuntimeConfig runtimeConfig) {

        if (runtimeConfig.sdkDisabled() || !runtimeConfig.instrument().jvmMetrics()) {
            runtimeTelemetry = null;
            return;
        }

        RuntimeTelemetryBuilder builder = RuntimeTelemetry.builder(openTelemetry);

        Internal.setEnableJfrFeature(builder, "CONTEXT_SWITCH_METRICS");
        Internal.setEnableJfrFeature(builder, "CPU_COUNT_METRICS");
        Internal.setEnableJfrFeature(builder, "LOCK_METRICS");
        Internal.setEnableJfrFeature(builder, "NETWORK_IO_METRICS");
        Internal.setDisableJfrFeature(builder, "MEMORY_POOL_METRICS");
        Internal.setUseLegacyJfrCpuCountMetric(builder, true);

        if (ImageMode.current().isNativeImage()) {
            Internal.setEnableJfrFeature(builder, "THREAD_METRICS");
            Internal.setEnableJfrFeature(builder, "CLASS_LOAD_METRICS");
            Internal.setEnableJfrFeature(builder, "GC_DURATION_METRICS");
            Internal.setEnableJfrFeature(builder, "CPU_UTILIZATION_METRICS");
            Internal.setEnableJfrFeature(builder, "MEMORY_ALLOCATION_METRICS");
        } else {
            Internal.setDisableJfrFeature(builder, "THREAD_METRICS");
            Internal.setDisableJfrFeature(builder, "CLASS_LOAD_METRICS");
            Internal.setDisableJfrFeature(builder, "GC_DURATION_METRICS");
            Internal.setDisableJfrFeature(builder, "CPU_UTILIZATION_METRICS");
            Internal.setDisableJfrFeature(builder, "MEMORY_ALLOCATION_METRICS");
        }

        runtimeTelemetry = builder.build();
    }

    @PreDestroy
    public void close() {
        if (runtimeTelemetry != null) {
            runtimeTelemetry.close();
        }
    }

}
