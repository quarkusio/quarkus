package io.quarkus.observability.common.config;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import io.quarkus.observability.common.ContainerConstants;
import io.quarkus.runtime.annotations.ConfigDocIgnore;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface LgtmConfig extends GrafanaConfig {

    /**
     * The name of the Grafana LGTM Docker image.
     */
    @WithDefault(ContainerConstants.LGTM)
    String imageName();

    /**
     * The Docker network aliases.
     */
    @WithDefault("lgtm,lgtm.testcontainer.docker")
    Optional<Set<String>> networkAliases();

    /**
     * The label of the container.
     */
    @WithDefault("quarkus-dev-service-lgtm")
    String label();

    /**
     * The value of the {@code quarkus-dev-service} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     */
    @WithDefault("lgtm")
    String serviceName();

    // this is duplicated for a reason - not all collectors speak grpc,
    // which is the default in OTEL exporter,
    // where we want http as a default with LGTM

    /**
     * Set of components to log.
     * Comma separated set of components whose container log we want to output.
     *
     * @return set of components to log
     */
    Optional<Set<LgtmComponent>> logging();

    /**
     * The LGTM's OTLP protocol.
     */
    @WithDefault(ContainerConstants.OTEL_HTTP_PROTOCOL)
    String otlpProtocol();

    /**
     * The (Prometheus) scraping interval, in seconds.
     */
    @WithDefault(ContainerConstants.SCRAPING_INTERVAL + "")
    int scrapingInterval();

    /**
     * Do we force scraping.
     */
    Optional<Boolean> forceScraping();

    /**
     * The gRPC port of the OTel container, if set it will be a fixed value.
     */
    OptionalInt otelGrpcPort();

    /**
     * The HTTP port of the OTel container, if set it will be a fixed value.
     */
    OptionalInt otelHttpPort();

    /**
     * Prometheus port, if set it will be a fixed value.
     */
    OptionalInt prometheusPort();

    /**
     * Tempo MCP port, if set it will be a fixed value.
     */
    OptionalInt tempoMcpPort();

    /**
     * The grace period in seconds for graceful shutdown after the container receives SIGTERM or SIGINT.
     */
    OptionalInt shutdownTimeout();

    /**
     * Enable OBI (OpenTelemetry eBPF Instrumentation) for automatic trace and RED metric generation.
     * Requires Linux kernel 5.8+ with BTF support. Enables {@code --pid=host} and {@code --privileged} on the container.
     */
    @WithDefault("false")
    boolean enableObi();

    /**
     * The OBI target to instrument. Can be a language ({@code java}, {@code python}, {@code node},
     * {@code dotnet}, {@code ruby}) or any regex matching an executable name.
     */
    Optional<String> obiTarget();

    /**
     * Override which ports OBI monitors for auto-instrumentation.
     */
    Optional<String> obiOpenPort();

    /**
     * Executable name pattern for OBI auto-instrumentation targeting.
     */
    Optional<String> obiAutoTargetExe();

    /**
     * Extra CLI arguments for Prometheus. The value is split on whitespace into separate arguments.
     */
    Optional<String> prometheusExtraArgs();

    /**
     * Extra CLI arguments for Loki. The value is split on whitespace into separate arguments.
     */
    Optional<String> lokiExtraArgs();

    /**
     * Extra CLI arguments for Tempo, appended after the default MCP enablement flag.
     * The value is split on whitespace into separate arguments.
     */
    Optional<String> tempoExtraArgs();

    /**
     * Extra CLI arguments for Pyroscope. The value is split on whitespace into separate arguments.
     */
    Optional<String> pyroscopeExtraArgs();

    /**
     * Extra CLI arguments for the OpenTelemetry Collector. The value is split on whitespace into separate arguments.
     */
    Optional<String> otelcolExtraArgs();

    /**
     * Classpath resource path to a custom Prometheus configuration file.
     * Mounted to {@code /otel-lgtm/prometheus.yaml} inside the container.
     */
    Optional<String> prometheusConfig();

    /**
     * Classpath resource path to a custom Loki configuration file.
     * Mounted to {@code /otel-lgtm/loki-config.yaml} inside the container.
     */
    Optional<String> lokiConfig();

    /**
     * Classpath resource path to a custom Tempo configuration file.
     * Mounted to {@code /otel-lgtm/tempo-config.yaml} inside the container.
     */
    Optional<String> tempoConfig();

    /**
     * Classpath resource path to a custom Pyroscope configuration file.
     * Mounted to {@code /otel-lgtm/pyroscope-config.yaml} inside the container.
     */
    Optional<String> pyroscopeConfig();

    /**
     * Classpath resource path to a custom OpenTelemetry Collector configuration file.
     * Mounted to {@code /otel-lgtm/otelcol-config.yaml} inside the container.
     */
    Optional<String> otelcolConfig();

    /**
     * Comma-separated list of Grafana plugins to pre-install.
     */
    Optional<String> grafanaPluginsPreinstall();

    /**
     * Path inside the container to a dashboard JSON file to use as the Grafana home dashboard.
     */
    Optional<String> grafanaHomeDashboardPath();

    /**
     * Global OTLP endpoint for forwarding logs, metrics, and traces to an external vendor via OTLP/HTTP.
     */
    Optional<String> vendorOtlpEndpoint();

    /**
     * Per-signal OTLP endpoint for forwarding logs to an external vendor. Takes precedence over the global endpoint.
     */
    Optional<String> vendorOtlpLogsEndpoint();

    /**
     * Per-signal OTLP endpoint for forwarding metrics to an external vendor. Takes precedence over the global endpoint.
     */
    Optional<String> vendorOtlpMetricsEndpoint();

    /**
     * Per-signal OTLP endpoint for forwarding traces to an external vendor. Takes precedence over the global endpoint.
     */
    Optional<String> vendorOtlpTracesEndpoint();

    /**
     * Authentication headers for the vendor OTLP endpoint (e.g. API keys or tokens).
     */
    Optional<String> vendorOtlpHeaders();

    /**
     * A way to override `quarkus.otel.metric.export.interval` property's default value.
     */
    @OverrideProperty("quarkus.otel.metric.export.interval")
    @WithDefault(ContainerConstants.OTEL_METRIC_EXPORT_INTERVAL)
    @ConfigDocIgnore
    String otelMetricExportInterval();

    /**
     * A way to override `quarkus.otel.bsp.schedule.delay` property's default value.
     */
    @OverrideProperty("quarkus.otel.bsp.schedule.delay")
    @WithDefault(ContainerConstants.OTEL_BSP_SCHEDULE_DELAY)
    @ConfigDocIgnore
    String otelBspScheduleDelay();

    /**
     * A way to override `quarkus.otel.metric.export.interval` property's default value.
     */
    @OverrideProperty("quarkus.otel.blrp.schedule.delay")
    @WithDefault(ContainerConstants.OTEL_BLRP_SCHEDULE_DELAY)
    @ConfigDocIgnore
    String otelBlrpScheduleDelay();
}
