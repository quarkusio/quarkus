package io.quarkus.opentelemetry.runtime.config.runtime.exporter;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.configuration.DurationConverter;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithName;

public interface OtlpExporterConfig {
    String DEFAULT_GRPC_BASE_URI = "http://localhost:4317/";
    String DEFAULT_HTTP_BASE_URI = "http://localhost:4318/";
    String DEFAULT_TIMEOUT_SECS = "10";

    /**
     * Sets the OTLP endpoint to send telemetry data. If unset, defaults to
     * {@value OtlpExporterRuntimeConfig#DEFAULT_GRPC_BASE_URI}.
     * <p>
     * There is a generic property, that will apply to all signals and a signal specific one, following the pattern:
     * `quarkus.otel.exporter.otlp.<signal-type>.endpoint` where <signal-type> is one of the supported signal types,
     * like `traces` or `metrics`.
     * <p>
     * If protocol is `http/protobuf` the version and signal will be appended to the path (e.g. v1/traces or v1/metrics)
     * and the default port will be {@value OtlpExporterRuntimeConfig#DEFAULT_HTTP_BASE_URI}.
     */
    @ConfigDocDefault(DEFAULT_GRPC_BASE_URI)
    Optional<String> endpoint();

    /**
     * Key-value pairs to be used as headers associated with exporter requests.
     * The format is similar to the {@code OTEL_EXPORTER_OTLP_HEADERS} environment variable,
     * a list of key-value pairs separated by the "=" character. i.e.: key1=value1,key2=value2
     * <p>
     * There is a generic property, that will apply to all signals and a signal specific one, following the pattern:
     * `quarkus.otel.exporter.otlp.<signal-type>.headers` where <signal-type> is one of the supported signal types,
     * like `traces` or `metrics`.
     */
    Optional<List<String>> headers();

    /**
     * Sets the method used to compress payloads. If unset, compression is disabled. Currently
     * supported compression methods include `gzip` and `none`.
     * <p>
     * There is a generic property, that will apply to all signals and a signal specific one, following the pattern:
     * `quarkus.otel.exporter.otlp.<signal-type>.compression` where <signal-type> is one of the supported signal types,
     * like `traces` or `metrics`.
     */
    Optional<CompressionType> compression();

    /**
     * Sets the maximum time to wait for the collector to process an exported batch of telemetry data. If
     * unset, defaults to {@value OtlpExporterRuntimeConfig#DEFAULT_TIMEOUT_SECS}s.
     * <p>
     * There is a generic property, that will apply to all signals and a signal specific one, following the pattern:
     * `quarkus.otel.exporter.otlp.<signal-type>.timeout` where <signal-type> is one of the supported signal types,
     * like `traces` or `metrics`.
     */
    @ConfigDocDefault("10s")
    @WithConverter(DurationConverter.class)
    Duration timeout();

    /**
     * OTLP defines the encoding of telemetry data and the protocol used to exchange data between the client and the
     * server. Depending on the exporter, the available protocols will be different.
     * <p>
     * Currently, only {@code grpc} and {@code http/protobuf} are allowed.
     * <p>
     * Please mind that changing the protocol requires changing the port in the endpoint as well.
     * <p>
     * There is a generic property, that will apply to all signals and a signal specific one, following the pattern:
     * `quarkus.otel.exporter.otlp.<signal-type>.protocol` where <signal-type> is one of the supported signal types,
     * like `traces` or `metrics`.
     */
    @ConfigDocDefault(OtlpExporterConfig.Protocol.GRPC)
    Optional<String> protocol();

    /**
     * Key/cert configuration in the PEM format.
     * <p>
     * There is a generic property, that will apply to all signals and a signal specific one, following the pattern:
     * `quarkus.otel.exporter.otlp.<signal-type>.key-cert` where <signal-type> is one of the supported signal types,
     * like `traces` or `metrics`.
     */
    @WithName("key-cert")
    KeyCert keyCert();

    /**
     * Trust configuration in the PEM format.
     * <p>
     * There is a generic property, that will apply to all signals and a signal specific one, following the pattern:
     * `quarkus.otel.exporter.otlp.<signal-type>.trust-cert` where <signal-type> is one of the supported signal types,
     * like `traces` or `metrics`.
     */
    @WithName("trust-cert")
    TrustCert trustCert();

    /**
     * The name of the TLS configuration to use.
     * <p>
     * If not set and the default TLS configuration is configured ({@code quarkus.tls.*}) then that will be used.
     * If a name is configured, it uses the configuration from {@code quarkus.tls.<name>.*}
     * If a name is configured, but no TLS configuration is found with that name then an error will be thrown.
     * <p>
     * There is a generic property, that will apply to all signals and a signal specific one, following the pattern:
     * `quarkus.otel.exporter.otlp.<signal-type>.tls-configuration-name` where <signal-type> is one of the supported signal
     * types,
     * like `traces` or `metrics`.
     */
    Optional<String> tlsConfigurationName();

    /**
     * Set proxy options.
     * <p>
     * There is a generic property, that will apply to all signals and a signal specific one, following the pattern:
     * `quarkus.otel.exporter.otlp.<signal-type>.proxy-options` where <signal-type> is one of the supported signal types,
     * like `traces` or `metrics`.
     */
    ProxyConfig proxyOptions();

    interface ProxyConfig {
        /**
         * If proxy connection must be used.
         * <p>
         * There is a generic property, that will apply to all signals and a signal specific one, following the pattern:
         * `quarkus.otel.exporter.otlp.<signal-type>.proxy-options.enabled` where <signal-type> is one of the supported signal
         * types,
         * like `traces` or `metrics`.
         */
        @ConfigDocDefault("false")
        boolean enabled();

        /**
         * Set proxy username.
         * <p>
         * There is a generic property, that will apply to all signals and a signal specific one, following the pattern:
         * `quarkus.otel.exporter.otlp.<signal-type>.proxy-options.username` where <signal-type> is one of the supported signal
         * types,
         * like `traces` or `metrics`.
         */
        Optional<String> username();

        /**
         * Set proxy password.
         * <p>
         * There is a generic property, that will apply to all signals and a signal specific one, following the pattern:
         * `quarkus.otel.exporter.otlp.<signal-type>.proxy-options.password` where <signal-type> is one of the supported signal
         * types,
         * like `traces` or `metrics`.
         */
        Optional<String> password();

        /**
         * Set proxy port.
         * <p>
         * There is a generic property, that will apply to all signals and a signal specific one, following the pattern:
         * `quarkus.otel.exporter.otlp.<signal-type>.proxy-options.port` where <signal-type> is one of the supported signal
         * types,
         * like `traces` or `metrics`.
         */
        OptionalInt port();

        /**
         * Set proxy host.
         * <p>
         * There is a generic property, that will apply to all signals and a signal specific one, following the pattern:
         * `quarkus.otel.exporter.otlp.<signal-type>.proxy-options.host` where <signal-type> is one of the supported signal
         * types,
         * like `traces` or `metrics`.
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
