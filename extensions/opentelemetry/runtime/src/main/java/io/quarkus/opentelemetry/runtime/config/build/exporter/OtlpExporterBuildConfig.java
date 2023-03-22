package io.quarkus.opentelemetry.runtime.config.build.exporter;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.otel.exporter.otlp")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface OtlpExporterBuildConfig {
    /**
     * Legacy property kept for compatibility reasons. Just the defining the right exporter is enough.
     * <p>
     * Maps to quarkus.opentelemetry.tracer.exporter.otlp.enabled and will be removed in the future
     */
    @Deprecated
    @WithDefault("true")
    boolean enabled();
}
