package io.quarkus.opentelemetry.runtime.config.build.exporter;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.otel.exporter.otlp")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface OtlpExporterBuildConfig {
    /**
     * Will disable the Quarkus managed OpenTelemetry exporters. No telemetry will be sent output if set to `false`.
     */
    @WithDefault("true")
    boolean enabled();
}
