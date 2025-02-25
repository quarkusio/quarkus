package io.quarkus.opentelemetry.runtime.config.runtime.exporter;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * From <a href=
 * "https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/protocol/exporter.md#configuration-options">
 * the OpenTelemetry Protocol Exporter configuration options</a>
 */
@ConfigMapping(prefix = "quarkus.otel.exporter.otlp")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface OtlpExporterRuntimeConfig extends OtlpExporterConfig {

    /**
     * OTLP traces exporter configuration.
     */
    OtlpExporterTracesConfig traces();

    /**
     * OTLP metrics exporter configuration.
     */
    OtlpExporterMetricsConfig metrics();

    /**
     * OTLP logs exporter configuration.
     */
    OtlpExporterLogsConfig logs();
}
