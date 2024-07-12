package io.quarkus.opentelemetry.runtime.config.runtime.exporter;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * From <a href=
 * "https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/protocol/exporter.md#configuration-options">
 * the OpenTelemetry Protocol Exporter configuration options</a>
 */
@ConfigMapping(prefix = "quarkus.otel.exporter.otlp")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface OtlpExporterRuntimeConfig extends OtlpExporterConfig {
    String DEFAULT_GRPC_BASE_URI = "http://localhost:4317/";
    String DEFAULT_HTTP_BASE_URI = "http://localhost:4318/";
    String DEFAULT_TIMEOUT_SECS = "10";

    /**
     * Sets the OTLP endpoint for connecting all signals. If unset, defaults to
     * {@value OtlpExporterRuntimeConfig#DEFAULT_GRPC_BASE_URI}.
     */
    @Override
    @WithDefault(DEFAULT_GRPC_BASE_URI)
    Optional<String> endpoint();

    /**
     * Key-value pairs to be used as headers associated with gRPC requests.
     * The format is similar to the {@code OTEL_EXPORTER_OTLP_HEADERS} environment variable,
     * a list of key-value pairs separated by the "=" character. i.e.: key1=value1,key2=value2
     */
    @Override
    Optional<List<String>> headers();

    /**
     * Sets the method used to compress payloads. If unset, compression is disabled. Currently
     * supported compression methods include `gzip` and `none`.
     */
    @Override
    Optional<CompressionType> compression();

    /**
     * Sets the maximum time to wait for the collector to process an exported batch of spans. If
     * unset, defaults to {@value OtlpExporterRuntimeConfig#DEFAULT_TIMEOUT_SECS}s.
     */
    @Override
    @WithDefault("10s")
    Duration timeout();

    /**
     * OTLP defines the encoding of telemetry data and the protocol used to exchange data between the client and the
     * server. Depending on the exporter, the available protocols will be different.
     * <p>
     * Currently, only {@code grpc} and {@code http/protobuf} are allowed.
     */
    @Override
    @WithDefault(OtlpExporterConfig.Protocol.GRPC)
    Optional<String> protocol();

    /**
     * Key/cert configuration in the PEM format.
     */
    @Override
    @WithName("key-cert")
    OtlpExporterConfig.KeyCert keyCert();

    /**
     * Trust configuration in the PEM format.
     */
    @Override
    @WithName("trust-cert")
    OtlpExporterConfig.TrustCert trustCert();

    /**
     * The name of the TLS configuration to use.
     * <p>
     * If not set and the default TLS configuration is configured ({@code quarkus.tls.*}) then that will be used.
     * If a name is configured, it uses the configuration from {@code quarkus.tls.<name>.*}
     * If a name is configured, but no TLS configuration is found with that name then an error will be thrown.
     */
    @Override
    Optional<String> tlsConfigurationName();

    /**
     * Set proxy options
     */
    @Override
    OtlpExporterConfig.ProxyConfig proxyOptions();

    /**
     * OTLP traces exporter configuration.
     */
    OtlpExporterTracesConfig traces();

    /**
     * OTLP metrics exporter configuration.
     */
    OtlpExporterMetricsConfig metrics();
    // TODO logs();
    // TODO additional global exporter configuration

}
