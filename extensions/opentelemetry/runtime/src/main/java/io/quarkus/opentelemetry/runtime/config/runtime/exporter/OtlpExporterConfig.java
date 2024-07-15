package io.quarkus.opentelemetry.runtime.config.runtime.exporter;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface OtlpExporterConfig {

    /**
     * OTLP Exporter specific. Will override <code>otel.exporter.otlp.endpoint</code>, if set.
     * <p>
     */
    Optional<String> endpoint();

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
    Duration timeout();

    /**
     * OTLP defines the encoding of telemetry data and the protocol used to exchange data between the client and the
     * server. Depending on the exporter, the available protocols will be different.
     * <p>
     * Currently, only {@code grpc} and {@code http/protobuf} are allowed.
     */
    Optional<String> protocol();

    /**
     * Key/cert configuration in the PEM format.
     */
    KeyCert keyCert();

    /**
     * Trust configuration in the PEM format.
     */
    TrustCert trustCert();

    /**
     * The name of the TLS configuration to use.
     * <p>
     * If not set and the default TLS configuration is configured ({@code quarkus.tls.*}) then that will be used.
     * If a name is configured, it uses the configuration from {@code quarkus.tls.<name>.*}
     * If a name is configured, but no TLS configuration is found with that name then an error will be thrown.
     */
    Optional<String> tlsConfigurationName();

    /**
     * Set proxy options
     */
    ProxyConfig proxyOptions();

    interface ProxyConfig {
        /**
         * If proxy connection must be used.
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * Set proxy username.
         */
        Optional<String> username();

        /**
         * Set proxy password.
         */
        Optional<String> password();

        /**
         * Set proxy port.
         */
        OptionalInt port();

        /**
         * Set proxy host.
         */
        Optional<String> host();
    }

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
