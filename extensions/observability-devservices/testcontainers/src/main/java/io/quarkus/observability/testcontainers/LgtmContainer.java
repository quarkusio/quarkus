package io.quarkus.observability.testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.MountableFile;

import io.quarkus.observability.common.ContainerConstants;
import io.quarkus.observability.common.config.AbstractGrafanaConfig;
import io.quarkus.observability.common.config.LgtmComponent;
import io.quarkus.observability.common.config.LgtmConfig;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.common.os.OS;

@SuppressWarnings("resource")
public class LgtmContainer extends GrafanaContainer<LgtmContainer, LgtmConfig> {
    protected static final String LGTM_NETWORK_ALIAS = "ltgm.testcontainer.docker";

    protected static final String PROMETHEUS_CONFIG_DEFAULT = """
            ---
            otlp:
              # Recommended attributes to be promoted to labels.
              promote_resource_attributes:
                - service.instance.id
                - service.name
                - service.namespace
                - service.version
                - cloud.availability_zone
                - cloud.region
                - container.name
                - deployment.environment.name
                - k8s.cluster.name
                - k8s.container.name
                - k8s.cronjob.name
                - k8s.daemonset.name
                - k8s.deployment.name
                - k8s.job.name
                - k8s.namespace.name
                - k8s.pod.name
                - k8s.replicaset.name
                - k8s.statefulset.name
            storage:
              tsdb:
                # A 10min time window is enough because it can easily absorb retries and network delays.
                out_of_order_time_window: 10m
            global:
              scrape_interval: %s
              evaluation_interval: 5s
            """;

    protected static final String PROMETHEUS_CONFIG_SCRAPE = """
            scrape_configs:
              - job_name: '%s'
                metrics_path: '%s%s'
                scrape_interval: %s
                static_configs:
                  - targets: ['%s:%d']
            """;

    protected static final String DASHBOARDS_CONFIG = """
            apiVersion: 1

            providers:
              - name: "Quarkus Micrometer Prometheus registry"
                type: file
                options:
                  path: /otel-lgtm/grafana-dashboard-quarkus-micrometer-prometheus.json
                  foldersFromFilesStructure: false
              - name: "Quarkus Micrometer OTLP registry"
                type: file
                options:
                  path: /otel-lgtm/grafana-dashboard-quarkus-micrometer-otlp.json
                  foldersFromFilesStructure: false
              - name: "Quarkus Micrometer OpenTelemetry"
                type: file
                options:
                  path: /otel-lgtm/grafana-dashboard-quarkus-micrometer-opentelemetry.json
                  foldersFromFilesStructure: false
              - name: "Quarkus OpenTelemetry logging"
                type: file
                options:
                  path: /otel-lgtm/grafana-dashboard-opentelemetry-logging.json
                  foldersFromFilesStructure: false
            """;

    private final boolean scrapingRequired;
    private final Set<String> customDashboards = new LinkedHashSet<>();

    public Set<String> getCustomDashboards() {
        return customDashboards;
    }

    public LgtmContainer(boolean scrapingRequired) {
        this(new LgtmConfigImpl(), scrapingRequired);
    }

    public LgtmContainer(LgtmConfig config, boolean scrapingRequired) {
        super(config);
        // do we require scraping
        this.scrapingRequired = scrapingRequired;
        // always expose all -- since the LGTM image already does that as well
        addExposedPorts(
                ContainerConstants.OTEL_GRPC_EXPORTER_PORT,
                ContainerConstants.OTEL_HTTP_EXPORTER_PORT,
                ContainerConstants.PROMETHEUS_PORT,
                ContainerConstants.TEMPO_MCP_PORT);
        config.otelGrpcPort().ifPresent(port -> addFixedExposedPort(port, ContainerConstants.OTEL_GRPC_EXPORTER_PORT));
        config.otelHttpPort().ifPresent(port -> addFixedExposedPort(port, ContainerConstants.OTEL_HTTP_EXPORTER_PORT));
        config.prometheusPort().ifPresent(port -> addFixedExposedPort(port, ContainerConstants.PROMETHEUS_PORT));
        config.tempoMcpPort().ifPresent(port -> addFixedExposedPort(port, ContainerConstants.TEMPO_MCP_PORT));

        String tempoArgs = "--query-frontend.mcp-server.enabled=true";
        Optional<String> extraTempoArgs = config.tempoExtraArgs();
        if (extraTempoArgs.isPresent()) {
            tempoArgs += " " + extraTempoArgs.get();
        }
        withEnv("TEMPO_EXTRA_ARGS", tempoArgs);
        config.prometheusExtraArgs().ifPresent(a -> withEnv("PROMETHEUS_EXTRA_ARGS", a));
        config.lokiExtraArgs().ifPresent(a -> withEnv("LOKI_EXTRA_ARGS", a));
        config.pyroscopeExtraArgs().ifPresent(a -> withEnv("PYROSCOPE_EXTRA_ARGS", a));
        config.otelcolExtraArgs().ifPresent(a -> withEnv("OTELCOL_EXTRA_ARGS", a));
        // allow for timeout shutdown, default is 5sec
        config.shutdownTimeout()
                .ifPresent(timeout -> withEnv("LGTM_SHUTDOWN_TIMEOUT_SECONDS", Integer.toString(timeout)));

        if (config.enableObi()) {
            withEnv("ENABLE_OBI", "true");
            withPrivilegedMode(true);
            withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withPidMode("host"));
            config.obiTarget().ifPresent(target -> withEnv("OBI_TARGET", target));
            config.obiOpenPort().ifPresent(port -> withEnv("OTEL_EBPF_OPEN_PORT", port));
            config.obiAutoTargetExe().ifPresent(exe -> withEnv("OTEL_EBPF_AUTO_TARGET_EXE", exe));
        }

        config.grafanaPluginsPreinstall().ifPresent(p -> withEnv("GF_PLUGINS_PREINSTALL", p));
        config.grafanaHomeDashboardPath()
                .ifPresent(p -> withEnv("GF_DASHBOARDS_DEFAULT_HOME_DASHBOARD_PATH", p));

        config.vendorOtlpEndpoint().ifPresent(e -> withEnv("OTEL_EXPORTER_OTLP_ENDPOINT", e));
        config.vendorOtlpLogsEndpoint().ifPresent(e -> withEnv("OTEL_EXPORTER_OTLP_LOGS_ENDPOINT", e));
        config.vendorOtlpMetricsEndpoint().ifPresent(e -> withEnv("OTEL_EXPORTER_OTLP_METRICS_ENDPOINT", e));
        config.vendorOtlpTracesEndpoint().ifPresent(e -> withEnv("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", e));
        config.vendorOtlpHeaders().ifPresent(h -> withEnv("OTEL_EXPORTER_OTLP_HEADERS", h));

        Optional<Set<LgtmComponent>> logging = config.logging();
        logging.ifPresent(set -> set.forEach(l -> withEnv("ENABLE_LOGS_" + l.name(), "true")));

        String dashboards = GrafanaUtils.consumeGrafanaResources(
                DASHBOARDS_CONFIG,
                s -> {
                    withCopyFileToContainer(
                            MountableFile.forClasspathResource(GrafanaUtils.META_INF_GRAFANA + "/" + s),
                            "/otel-lgtm/" + s);
                    customDashboards.add(s);
                });

        // Replacing bundled dashboards with our own
        addFileToContainer(dashboards.getBytes(StandardCharsets.UTF_8),
                "/otel-lgtm/grafana/conf/provisioning/dashboards/grafana-dashboards.yaml");

        withCopyFileToContainer(
                MountableFile.forClasspathResource("/grafana-dashboard-quarkus-micrometer-prometheus.json"),
                "/otel-lgtm/grafana-dashboard-quarkus-micrometer-prometheus.json");
        withCopyFileToContainer(
                MountableFile.forClasspathResource("/grafana-dashboard-quarkus-micrometer-otlp.json"),
                "/otel-lgtm/grafana-dashboard-quarkus-micrometer-otlp.json");
        withCopyFileToContainer(
                MountableFile.forClasspathResource("/grafana-dashboard-quarkus-micrometer-opentelemetry.json"),
                "/otel-lgtm/grafana-dashboard-quarkus-micrometer-opentelemetry.json");
        withCopyFileToContainer(
                MountableFile.forClasspathResource("/grafana-dashboard-opentelemetry-logging.json"),
                "/otel-lgtm/grafana-dashboard-opentelemetry-logging.json");

        if (config.prometheusConfig().isPresent()) {
            withCopyFileToContainer(
                    MountableFile.forClasspathResource(config.prometheusConfig().get()),
                    "/otel-lgtm/prometheus.yaml");
        } else {
            addFileToContainer(getPrometheusConfig().getBytes(), "/otel-lgtm/prometheus.yaml");
        }
        config.lokiConfig().ifPresent(r -> withCopyFileToContainer(
                MountableFile.forClasspathResource(r), "/otel-lgtm/loki-config.yaml"));
        config.tempoConfig().ifPresent(r -> withCopyFileToContainer(
                MountableFile.forClasspathResource(r), "/otel-lgtm/tempo-config.yaml"));
        config.pyroscopeConfig().ifPresent(r -> withCopyFileToContainer(
                MountableFile.forClasspathResource(r), "/otel-lgtm/pyroscope-config.yaml"));
        config.otelcolConfig().ifPresent(r -> withCopyFileToContainer(
                MountableFile.forClasspathResource(r), "/otel-lgtm/otelcol-config.yaml"));
    }

    @Override
    protected WaitStrategy waitStrategy() {
        return new WaitAllStrategy()
                .withStartupTimeout(config.timeout())
                .withStrategy(super.waitStrategy())
                .withStrategy(
                        Wait.forLogMessage(".*(The OpenTelemetry collector and the Grafana LGTM stack are up and running|" +
                                "All components are up and running).*", 1)
                                .withStartupTimeout(config.timeout()));
    }

    @Override
    protected String prefix() {
        return "LGTM";
    }

    @Override
    protected Predicate<OutputFrame> getLoggingFilter() {
        return new LgtmLoggingFilter();
    }

    public String getOtlpProtocol() {
        return config.otlpProtocol();
    }

    private int getPrivateOtlpPort() {
        return getPrivateOtlpPort(getOtlpProtocol());
    }

    public static int getPrivateOtlpPort(String otlpProtocol) {
        // use ignore-case here; grpc == gRPC
        if (ContainerConstants.OTEL_GRPC_PROTOCOL.equalsIgnoreCase(otlpProtocol)) {
            return ContainerConstants.OTEL_GRPC_EXPORTER_PORT;
        } else if (ContainerConstants.OTEL_HTTP_PROTOCOL.equals(otlpProtocol)) {
            return ContainerConstants.OTEL_HTTP_EXPORTER_PORT;
        } else {
            throw new IllegalArgumentException("Unsupported OTEL protocol: " + otlpProtocol);
        }
    }

    private String getPrometheusConfig() {
        String scraping = config.scrapingInterval() + "s";
        String prometheusConfig = String.format(PROMETHEUS_CONFIG_DEFAULT, scraping);
        if (config.forceScraping().orElse(scrapingRequired)) {
            boolean isTest = LaunchMode.current() == LaunchMode.TEST;
            Config runtimeConfig = ConfigProvider.getConfig();
            String rootPath = runtimeConfig.getOptionalValue("quarkus.management.root-path", String.class).orElse("/q");
            String metricsPath = runtimeConfig.getOptionalValue("quarkus.management.metrics.path", String.class)
                    .orElse("/metrics");
            String httpPortKey = isTest ? "quarkus.http.test-port" : "quarkus.http.port";
            Optional<Integer> optionalValue = runtimeConfig.getOptionalValue(httpPortKey, Integer.class);
            int httpPort = optionalValue.orElse(isTest ? 8081 : 8080); // when not set use default

            // On Linux, you can’t automatically resolve host.docker.internal,
            // you need to provide the following run flag when you start the container:
            //--add-host=host.docker.internal:host-gateway
            if (OS.current() == OS.LINUX) {
                withCreateContainerCmdModifier(cmd -> cmd
                        .getHostConfig()
                        .withExtraHosts("host.docker.internal:host-gateway"));
            }

            prometheusConfig += String.format(PROMETHEUS_CONFIG_SCRAPE, config.serviceName(), rootPath, metricsPath, scraping,
                    "host.docker.internal", httpPort);
        }
        return prometheusConfig;
    }

    protected static class LgtmConfigImpl extends AbstractGrafanaConfig implements LgtmConfig {
        public LgtmConfigImpl() {
            this(ContainerConstants.LGTM);
        }

        public LgtmConfigImpl(String imageName) {
            super(imageName);
        }

        @Override
        public Optional<Set<String>> networkAliases() {
            return Optional.of(Set.of("lgtm", LGTM_NETWORK_ALIAS));
        }

        @Override
        public Optional<Set<LgtmComponent>> logging() {
            return Optional.empty();
        }

        @Override
        public String otlpProtocol() {
            return ContainerConstants.OTEL_HTTP_PROTOCOL;
        }

        @Override
        public int scrapingInterval() {
            return ContainerConstants.SCRAPING_INTERVAL;
        }

        @Override
        public Optional<Boolean> forceScraping() {
            return Optional.empty();
        }

        @Override
        public OptionalInt otelGrpcPort() {
            return OptionalInt.empty();
        }

        @Override
        public OptionalInt otelHttpPort() {
            return OptionalInt.empty();
        }

        @Override
        public OptionalInt prometheusPort() {
            return OptionalInt.empty();
        }

        @Override
        public OptionalInt tempoMcpPort() {
            return OptionalInt.empty();
        }

        @Override
        public OptionalInt shutdownTimeout() {
            return OptionalInt.empty();
        }

        @Override
        public boolean enableObi() {
            return false;
        }

        @Override
        public Optional<String> obiTarget() {
            return Optional.empty();
        }

        @Override
        public Optional<String> obiOpenPort() {
            return Optional.empty();
        }

        @Override
        public Optional<String> obiAutoTargetExe() {
            return Optional.empty();
        }

        @Override
        public Optional<String> prometheusExtraArgs() {
            return Optional.empty();
        }

        @Override
        public Optional<String> lokiExtraArgs() {
            return Optional.empty();
        }

        @Override
        public Optional<String> tempoExtraArgs() {
            return Optional.empty();
        }

        @Override
        public Optional<String> pyroscopeExtraArgs() {
            return Optional.empty();
        }

        @Override
        public Optional<String> otelcolExtraArgs() {
            return Optional.empty();
        }

        @Override
        public Optional<String> prometheusConfig() {
            return Optional.empty();
        }

        @Override
        public Optional<String> lokiConfig() {
            return Optional.empty();
        }

        @Override
        public Optional<String> tempoConfig() {
            return Optional.empty();
        }

        @Override
        public Optional<String> pyroscopeConfig() {
            return Optional.empty();
        }

        @Override
        public Optional<String> otelcolConfig() {
            return Optional.empty();
        }

        @Override
        public Optional<String> grafanaPluginsPreinstall() {
            return Optional.empty();
        }

        @Override
        public Optional<String> grafanaHomeDashboardPath() {
            return Optional.empty();
        }

        @Override
        public Optional<String> vendorOtlpEndpoint() {
            return Optional.empty();
        }

        @Override
        public Optional<String> vendorOtlpLogsEndpoint() {
            return Optional.empty();
        }

        @Override
        public Optional<String> vendorOtlpMetricsEndpoint() {
            return Optional.empty();
        }

        @Override
        public Optional<String> vendorOtlpTracesEndpoint() {
            return Optional.empty();
        }

        @Override
        public Optional<String> vendorOtlpHeaders() {
            return Optional.empty();
        }

        @Override
        public String otelMetricExportInterval() {
            return ContainerConstants.OTEL_METRIC_EXPORT_INTERVAL;
        }

        @Override
        public String otelBspScheduleDelay() {
            return ContainerConstants.OTEL_BSP_SCHEDULE_DELAY;
        }

        @Override
        public String otelBlrpScheduleDelay() {
            return ContainerConstants.OTEL_BLRP_SCHEDULE_DELAY;
        }
    }

    protected static class LgtmLoggingFilter implements Predicate<OutputFrame> {
        @Override
        public boolean test(OutputFrame outputFrame) {
            final var line = outputFrame.getUtf8StringWithoutLineEnding();
            return !(line.startsWith("Waiting for") && line.endsWith("to start up..."));
        }
    }
}
