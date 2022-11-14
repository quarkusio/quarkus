package io.quarkus.opentelemetry.runtime.config.build.exporter;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(prefix = "otel.exporter.otlp", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class OtlpExporterBuildConfig {
    /**
     * Legacy property kept for compatibility reasons. Just the defining the right exporter is enough.
     * <p>
     * Maps to quarkus.opentelemetry.tracer.exporter.otlp.enabled and will be removed in the future
     */
    @Deprecated()
    @ConfigItem(defaultValue = "${quarkus.opentelemetry.tracer.exporter.otlp.enabled:true}")
    public boolean enabled;
}
