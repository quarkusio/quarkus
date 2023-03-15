package io.quarkus.opentelemetry.runtime.config.runtime.exporter;

import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig.Constants.DEFAULT_GRPC_BASE_URI;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class OtlpExporterTracesConfig extends OtelConnectionRuntimeConfig {

    /**
     * OTLP Exporter specific. Will override otel.exporter.otlp.endpoint, if set.
     * <p>
     * The old Quarkus configuration used the opentelemetry.tracer.exporter.otlp.endpoint system property
     * to define the full endpoint starting with http or https.
     * If the old property is set, we will use it until the transition period ends.
     * <p>
     * Default value is {@value OtlpExporterRuntimeConfig.Constants#DEFAULT_GRPC_BASE_URI}
     */
    //    @WithConverter(TrimmedStringConverter.class)
    @ConfigItem(defaultValue = DEFAULT_GRPC_BASE_URI)
    public Optional<String> endpoint;

    /**
     * This is
     * See {@link OtlpExporterTracesConfig#endpoint}
     */
    //    @WithConverter(TrimmedStringConverter.class)
    @Deprecated
    @ConfigItem(name = "legacy-endpoint", defaultValue = "${quarkus.opentelemetry.tracer.exporter.otlp.endpoint:" +
            DEFAULT_GRPC_BASE_URI + "}")
    public Optional<String> legacyEndpoint;

    //    /**
    //     * Sets the certificate chain to use for verifying servers when TLS is enabled. The {@code byte[]}
    //     * should contain an X.509 certificate collection in PEM format. If not set, TLS connections will
    //     * use the system default trusted certificates.
    //     */
    //    @ConfigItem()
    //    public Optional<byte[]> certificate;

    //    /**
    //     * Sets ths client key and the certificate chain to use for verifying client when TLS is enabled.
    //     * The key must be PKCS8, and both must be in PEM format.
    //     */
    //    @ConfigItem()
    //    public Optional<OtlpExporterRuntimeConfig.ClientTlsConfig> client;

    /**
     * Key-value pairs to be used as headers associated with gRPC requests.
     * The format is similar to the {@code OTEL_EXPORTER_OTLP_HEADERS} environment variable,
     * a list of key-value pairs separated by the "=" character.
     * See <a href=
     * "https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/protocol/exporter.md#specifying-headers-via-environment-variables">
     * Specifying headers</a> for more details.
     */
    public Map<String, String> headers;

    /**
     * Sets the method used to compress payloads. If unset, compression is disabled. Currently
     * supported compression methods include "gzip" and "none".
     */
    @ConfigItem()
    public Optional<CompressionType> compression;

    /**
     * Sets the maximum time to wait for the collector to process an exported batch of spans. If
     * unset, defaults to {@value OtelConnectionRuntimeConfig.Constants#DEFAULT_TIMEOUT_SECS}s.
     */
    @ConfigItem(defaultValue = "10S")
    public Duration timeout;

    /**
     * OTLP defines the encoding of telemetry data and the protocol used to exchange data between the client and the server.
     * Depending on the exporter, the available protocols will be different.
     */
    @ConfigItem(defaultValue = Protocol.HTTP_PROTOBUF)
    public Optional<String> protocol;

    public static class Protocol {
        public static final String GRPC = "grpc";
        public static final String HTTP_PROTOBUF = "http/protobuf";
        public static final String HTTP_JSON = "http/json";
    }
}
