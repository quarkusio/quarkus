package io.quarkus.opentelemetry.exporter.otlp.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "otel.exporter.otlp")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface OtlpExporterBuildConfig {

    /**
     * Legacy property kept for compatibility reasons. Just the defining the right exporter is enough.
     * <p>
     * Maps to quarkus.opentelemetry.tracer.exporter.otlp.enabled and will be removed in the future
     */
    @Deprecated()
    @WithDefault("${quarkus.opentelemetry.tracer.exporter.otlp.enabled:true}")
    Optional<Boolean> enabled();
}
