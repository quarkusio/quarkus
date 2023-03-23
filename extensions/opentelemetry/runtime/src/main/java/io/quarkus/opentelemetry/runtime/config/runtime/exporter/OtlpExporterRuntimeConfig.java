package io.quarkus.opentelemetry.runtime.config.runtime.exporter;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "otel.exporter.otlp", phase = ConfigPhase.RUN_TIME)
public class OtlpExporterRuntimeConfig {

    /**
     * Sets the OTLP endpoint to connect to. If unset, defaults to {@value Constants#DEFAULT_GRPC_BASE_URI}.
     * We are currently using just the traces, therefore traces.endpoint is recommended.
     */
    //    @WithConverter(TrimmedStringConverter.class)
    @ConfigItem(defaultValue = Constants.DEFAULT_GRPC_BASE_URI)
    public Optional<String> endpoint;

    /**
     * OTLP traces exporter configuration
     */
    public OtlpExporterTracesConfig traces;
    // TODO metrics();
    // TODO logs();
    // TODO additional global exporter configuration

    /**
     * From <a href=
     * "https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/protocol/exporter.md#configuration-options">
     * the OpenTelemetry Protocol Exporter configuration options</a>
     */
    public class Constants {
        public static final String DEFAULT_GRPC_BASE_URI = "http://localhost:4317/";
        public static final String DEFAULT_HTTP_BASE_URI = "http://localhost:4318/";
    }
}
