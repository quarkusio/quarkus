package io.quarkus.opentelemetry.exporter.otlp.runtime;

import static io.quarkus.opentelemetry.exporter.otlp.runtime.OtlpExporterRuntimeConfig.Constants.DEFAULT_GRPC_BASE_URL;
import static io.quarkus.opentelemetry.exporter.otlp.runtime.OtlpExporterTracesConfig.Constants.DEFAULT_GRPC_PATH;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import io.quarkus.opentelemetry.runtime.config.OtelConnectionConfig;
import io.quarkus.opentelemetry.runtime.tracing.config.TracesExporterConfig;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

public interface OtlpExporterTracesConfig extends TracesExporterConfig {

    /**
     * OTLP Exporter specific. Will be concatenated after otel.exporter.otlp.endpoint.
     * <p>
     * The old Quarkus configuration used the opentelemetry.tracer.exporter.otlp.endpoint system property
     * to define the full endpoint starting with http or https.
     * If the old property is set, we will use it until the transition period ends.
     * <p>
     * Default value is {@value OtlpExporterTracesConfig.Constants#DEFAULT_GRPC_PATH}
     */
    //    @WithConverter(TrimmedStringConverter.class)
    @WithDefault(DEFAULT_GRPC_PATH)
    @Override
    Optional<String> endpoint();

    /**
     * See {@link OtlpExporterTracesConfig#endpoint()}
     */
    @Deprecated
    //    @WithConverter(TrimmedStringConverter.class)
    @WithDefault("${quarkus.opentelemetry.tracer.exporter.otlp.endpoint:" + DEFAULT_GRPC_BASE_URL + DEFAULT_GRPC_PATH + "}")
    @WithName("legacy-endpoint")
    Optional<String> legacyEndpoint();

    /**
     * Key-value pairs to be used as headers associated with gRPC requests.
     * The format is similar to the {@code OTEL_EXPORTER_OTLP_HEADERS} environment variable,
     * a list of key-value pairs separated by the "=" character.
     * See <a href=
     * "https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/protocol/exporter.md#specifying-headers-via-environment-variables">
     * Specifying headers</a> for more details.
     */
    @Override
    Map<String, String> headers();

    /**
     * Sets the maximum time to wait for the collector to process an exported batch of spans. If
     * unset, defaults to {@value OtelConnectionConfig.Constants#DEFAULT_TIMEOUT_SECS}s.
     */
    @Override
    @WithDefault("10S")
    Duration timeout();

    @Override
    @WithDefault(Protocol.HTTP_PROTOBUF)
    Optional<String> protocol();

    /**
     * From <a href=
     * "https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/protocol/exporter.md#configuration-options">
     * the OpenTelemetry Protocol Exporter configuration options</a>
     */
    class Constants {
        public static final String DEFAULT_GRPC_PATH = "v1/traces";
    }

    class Protocol {
        public static final String GRPC = "grpc";
        public static final String HTTP_PROTOBUF = "http/protobuf";
        public static final String HTTP_JSON = "http/json";
    }
}
