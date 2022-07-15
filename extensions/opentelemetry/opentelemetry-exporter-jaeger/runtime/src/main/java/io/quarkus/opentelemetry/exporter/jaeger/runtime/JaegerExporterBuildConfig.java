package io.quarkus.opentelemetry.exporter.jaeger.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "otel.exporter.jaeger")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface JaegerExporterBuildConfig {

    /**
     * Legacy property kept for compatibility reasons. Just the defining the right exporter is enough.
     * <p>
     * Maps to quarkus.opentelemetry.tracer.exporter.jaeger.enabled and will be removed in the future
     */
    @Deprecated()
    @WithDefault("${quarkus.opentelemetry.tracer.exporter.jaeger.enabled:true}")
    Optional<Boolean> enabled();
}
