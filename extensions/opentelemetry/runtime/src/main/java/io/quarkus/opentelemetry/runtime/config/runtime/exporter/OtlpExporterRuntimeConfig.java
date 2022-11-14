package io.quarkus.opentelemetry.runtime.config.runtime.exporter;

import java.time.Duration;
import java.util.Map;
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
    //    public Optional<ClientTlsConfig> client;

    /**
     * Add header to request. Optional.
     */
    @ConfigItem()
    public Map<String, String> headers;

    /**
     * Sets the method used to compress payloads. If unset, compression is disabled. Currently
     * supported compression methods include "gzip" and "none".
     */
    @ConfigItem()
    public Optional<OtelConnectionRuntimeConfig.CompressionType> compression;

    /**
     * Sets the maximum time to wait for the collector to process an exported batch of spans. If
     * unset, defaults to {@value OtelConnectionRuntimeConfig.Constants#DEFAULT_TIMEOUT_SECS}s.
     */
    @ConfigItem(defaultValue = OtelConnectionRuntimeConfig.Constants.DEFAULT_TIMEOUT_SECS)
    public Duration timeout;

    /**
     * OTLP defines the encoding of telemetry data and the protocol used to exchange data between the client and the server.
     * Depending on the exporter, the available protocols will be different.
     */
    @ConfigItem()
    public Optional<String> protocol;

    //    @ConfigGroup
    //    public class ClientTlsConfig {
    //
    //        /**
    //         * Key
    //         */
    //        @ConfigItem()
    //        public byte[] key;
    //
    //        /**
    //         * Certificate
    //         */
    //        @ConfigItem()
    //        public byte[] certificate;
    //    }

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
