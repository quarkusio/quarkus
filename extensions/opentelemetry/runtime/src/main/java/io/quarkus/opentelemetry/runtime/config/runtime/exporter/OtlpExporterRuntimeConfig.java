package io.quarkus.opentelemetry.runtime.config.runtime.exporter;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * From <a href=
 * "https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/protocol/exporter.md#configuration-options">
 * the OpenTelemetry Protocol Exporter configuration options</a>
 */
@ConfigMapping(prefix = "quarkus.otel.exporter.otlp")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface OtlpExporterRuntimeConfig {
    String DEFAULT_GRPC_BASE_URI = "http://localhost:4317/";
    String DEFAULT_HTTP_BASE_URI = "http://localhost:4318/";
    String DEFAULT_TIMEOUT_SECS = "10";

    /**
     * Sets the OTLP endpoint to connect to. If unset, defaults to {@value OtlpExporterRuntimeConfig#DEFAULT_GRPC_BASE_URI}.
     * We are currently using just the traces, therefore <code>quarkus.otel.exporter.otlp.traces.endpoint</code>
     * is recommended.
     */
    @WithDefault(DEFAULT_GRPC_BASE_URI)
    Optional<String> endpoint();

    /**
     * OTLP traces exporter configuration.
     */
    OtlpExporterTracesConfig traces();
    // TODO metrics();
    // TODO logs();
    // TODO additional global exporter configuration

}
