package io.quarkus.observability.testcontainers;

import java.util.Optional;
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
import io.quarkus.observability.common.config.LgtmConfig;

public class LgtmContainer extends GrafanaContainer<LgtmContainer, LgtmConfig> {
    protected static final String LGTM_NETWORK_ALIAS = "ltgm.testcontainer.docker";

    protected static final String PROMETHEUS_CONFIG = """
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
              scrape_interval: 5s
              evaluation_interval: 5s
            scrape_configs:
              - job_name: '%s'
                metrics_path: '%s%s'
                scrape_interval: 5s
                static_configs:
                  - targets: ['%s:%d']
            """;

    protected static final String DASHBOARDS_CONFIG = """
            apiVersion: 1

            providers:
              - name: "Quarkus Micrometer Prometheus"
                type: file
                options:
                  path: /otel-lgtm/grafana-dashboard-quarkus-micrometer-prometheus.json
                  foldersFromFilesStructure: false
              - name: "Quarkus Micrometer with OTLP output"
                type: file
                options:
                  path: /otel-lgtm/grafana-dashboard-quarkus-micrometer-otlp.json
                  foldersFromFilesStructure: false
            """;

    public LgtmContainer() {
        this(new LgtmConfigImpl());
    }

    public LgtmContainer(LgtmConfig config) {
        super(config);
        // always expose both -- since the LGTM image already does that as well
        addExposedPorts(ContainerConstants.OTEL_GRPC_EXPORTER_PORT, ContainerConstants.OTEL_HTTP_EXPORTER_PORT);

        // Replacing bundled dashboards with our own
        addFileToContainer(DASHBOARDS_CONFIG.getBytes(),
                "/otel-lgtm/grafana/conf/provisioning/dashboards/grafana-dashboards.yaml");
        withCopyFileToContainer(
                MountableFile.forClasspathResource("/grafana-dashboard-quarkus-micrometer-prometheus.json"),
                "/otel-lgtm/grafana-dashboard-quarkus-micrometer-prometheus.json");
        withCopyFileToContainer(
                MountableFile.forClasspathResource("/grafana-dashboard-quarkus-micrometer-otlp.json"),
                "/otel-lgtm/grafana-dashboard-quarkus-micrometer-otlp.json");
        addFileToContainer(getPrometheusConfig().getBytes(), "/otel-lgtm/prometheus.yaml");

    }

    @Override
    protected WaitStrategy waitStrategy() {
        return new WaitAllStrategy()
                .withStartupTimeout(config.timeout())
                .withStrategy(super.waitStrategy())
                .withStrategy(
                        Wait.forLogMessage(".*The OpenTelemetry collector and the Grafana LGTM stack are up and running.*", 1)
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

    public int getOtlpPort() {
        int port = getOtlpPortInternal();
        return getMappedPort(port);
    }

    private int getOtlpPortInternal() {
        // use ignore-case here; grpc == gRPC
        if (ContainerConstants.OTEL_GRPC_PROTOCOL.equalsIgnoreCase(getOtlpProtocol())) {
            return ContainerConstants.OTEL_GRPC_EXPORTER_PORT;
        } else if (ContainerConstants.OTEL_HTTP_PROTOCOL.equals(getOtlpProtocol())) {
            return ContainerConstants.OTEL_HTTP_EXPORTER_PORT;
        } else {
            throw new IllegalArgumentException("Unsupported OTEL protocol: " + getOtlpProtocol());
        }
    }

    private String getPrometheusConfig() {
        Config runtimeConfig = ConfigProvider.getConfig();
        String rootPath = runtimeConfig.getOptionalValue("quarkus.management.root-path", String.class).orElse("/q");
        String metricsPath = runtimeConfig.getOptionalValue("quarkus.management.metrics.path", String.class).orElse("/metrics");
        int httpPort = runtimeConfig.getOptionalValue("quarkus.http.port", Integer.class).orElse(8080); // when not set use default

        return String.format(PROMETHEUS_CONFIG, config.serviceName(), rootPath, metricsPath, "host.docker.internal", httpPort);
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
        public String otlpProtocol() {
            return ContainerConstants.OTEL_HTTP_PROTOCOL;
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
