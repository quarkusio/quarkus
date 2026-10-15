package io.quarkus.opentelemetry.runtime.metrics.instrumentation;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.config.IncludeExclude;
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

        // JMX metrics are emitted by default. The selectors below opt-in to the JFR based metrics
        // that complement them (they are OpenTelemetry metric names, wildcards allowed). The JFR
        // memory pool metrics are intentionally left out so that memory is sourced from JMX.
        List<String> includedJfrMetrics = new ArrayList<>(List.of(
                "jvm.cpu.context_switch",
                "jvm.cpu.limit",
                "jvm.cpu.longlock",
                "jvm.network.*"));

        if (ImageMode.current().isNativeImage()) {
            // In native mode some of the JMX sourced metrics are not available, so their JFR
            // equivalents are enabled instead. Note the CPU utilization metric is the whole-system
            // JFR one (jvm.system.cpu.utilization) rather than the JMX jvm.cpu.recent_utilization.
            includedJfrMetrics.add("jvm.thread.count");
            includedJfrMetrics.add("jvm.class.*");
            includedJfrMetrics.add("jvm.gc.duration");
            includedJfrMetrics.add("jvm.system.cpu.utilization");
            includedJfrMetrics.add("jvm.memory.allocation");
        }

        Internal.setJfrMetrics(builder, IncludeExclude.builder()
                .setIncluded(includedJfrMetrics)
                .build());
        Internal.setUseLegacyJfrCpuCountMetric(builder, true);

        runtimeTelemetry = builder.build();
    }

    @PreDestroy
    public void close() {
        if (runtimeTelemetry != null) {
            runtimeTelemetry.close();
        }
    }

}
