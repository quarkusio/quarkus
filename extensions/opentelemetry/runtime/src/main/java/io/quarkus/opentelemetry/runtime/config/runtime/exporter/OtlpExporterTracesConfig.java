package io.quarkus.opentelemetry.runtime.config.runtime.exporter;

import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig.DEFAULT_GRPC_BASE_URI;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigGroup
public interface OtlpExporterTracesConfig {

    /**
     * OTLP Exporter specific. Will override <code>otel.exporter.otlp.endpoint</code>, if set.
     * <p>
     * Fallbacks to the legacy property <code>quarkus.opentelemetry.tracer.exporter.otlp.endpoint<</code> or
     * defaults to {@value OtlpExporterRuntimeConfig#DEFAULT_GRPC_BASE_URI}.
     */
    @WithDefault(DEFAULT_GRPC_BASE_URI)
    Optional<String> endpoint();

    /**
     * See {@link OtlpExporterTracesConfig#endpoint}
     */
    //    @WithConverter(TrimmedStringConverter.class)
    @Deprecated
    @WithName("legacy-endpoint")
    @WithDefault(DEFAULT_GRPC_BASE_URI)
    Optional<String> legacyEndpoint();

    /**
     * Key-value pairs to be used as headers associated with gRPC requests.
     * The format is similar to the {@code OTEL_EXPORTER_OTLP_HEADERS} environment variable,
     * a list of key-value pairs separated by the "=" character. i.e.: key1=value1,key2=value2
     */
    Optional<List<String>> headers();

    /**
     * Sets the method used to compress payloads. If unset, compression is disabled. Currently
     * supported compression methods include `gzip` and `none`.
     */
    Optional<CompressionType> compression();

    /**
     * Sets the maximum time to wait for the collector to process an exported batch of spans. If
     * unset, defaults to {@value OtlpExporterRuntimeConfig#DEFAULT_TIMEOUT_SECS}s.
     */
    @WithDefault("10s")
    Duration timeout();

    /**
     * OTLP defines the encoding of telemetry data and the protocol used to exchange data between the client and the
     * server. Depending on the exporter, the available protocols will be different.
     * <p>
     * Currently, only {@code grpc} and {@code http/protobuf} are allowed.
     */
    @WithDefault(Protocol.GRPC)
    Optional<String> protocol();

    /**
     * Key/cert configuration in the PEM format.
     */
    @WithName("key-cert")
    KeyCert keyCert();

    /**
     * Trust configuration in the PEM format.
     */
    @WithName("trust-cert")
    TrustCert trustCert();

    interface KeyCert {
        /**
         * Comma-separated list of the path to the key files (Pem format).
         */
        Optional<List<String>> keys();

        /**
         * Comma-separated list of the path to the certificate files (Pem format).
         */
        Optional<List<String>> certs();
    }

    interface TrustCert {
        /**
         * Comma-separated list of the trust certificate files (Pem format).
         */
        Optional<List<String>> certs();
    }

    class Protocol {
        public static final String GRPC = "grpc";
        public static final String HTTP_PROTOBUF = "http/protobuf";
    }
}
