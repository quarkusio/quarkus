package io.quarkus.observability.testcontainers;

import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.utility.MountableFile;

import io.quarkus.devservices.common.JBossLoggingConsumer;
import io.quarkus.observability.common.ContainerConstants;
import io.quarkus.observability.common.config.AbstractGrafanaConfig;
import io.quarkus.observability.common.config.LgtmConfig;

public class LgtmContainer extends GrafanaContainer<LgtmContainer, LgtmConfig> {
    /**
     * Logger which will be used to capture container STDOUT and STDERR.
     */
    private static final Logger log = Logger.getLogger(LgtmContainer.class);

    protected static final String LGTM_NETWORK_ALIAS = "ltgm.testcontainer.docker";

    protected static final String PROMETHEUS_CONFIG = """
                global:
                  scrape_interval: 10s
                  evaluation_interval: 10s
                storage:
                  tsdb:
                    out_of_order_time_window: 10m
                scrape_configs:
                  - job_name: '%s'
                    metrics_path: '%s%s'
                    scrape_interval: 10s
                    static_configs:
                      - targets: ['%s:%d']
            """;

    public LgtmContainer() {
        this(new LgtmConfigImpl());
    }

    public LgtmContainer(LgtmConfig config) {
        super(config);
        // always expose both -- since the LGTM image already does that as well
        addExposedPorts(ContainerConstants.OTEL_GRPC_EXPORTER_PORT, ContainerConstants.OTEL_HTTP_EXPORTER_PORT);
        // cannot override grafana-dashboards.yaml in the container because it's on a version dependent path:
        // ./grafana-v11.0.0/conf/provisioning/dashboards/grafana-dashboards.yaml
        // will replace contents of current dashboards
        withLogConsumer(new JBossLoggingConsumer(log).withPrefix("LGTM"));
        withCopyFileToContainer(
                MountableFile.forClasspathResource("/grafana-dashboard-quarkus-micrometer-prometheus.json"),
                "/otel-lgtm/grafana-dashboard-red-metrics-classic.json");
        withCopyFileToContainer(
                MountableFile.forClasspathResource("/grafana-dashboard-quarkus-micrometer-otlp.json"),
                "/otel-lgtm/grafana-dashboard-red-metrics-native.json");
        withCopyFileToContainer(
                MountableFile.forClasspathResource("/empty.json"),
                "/otel-lgtm/grafana-dashboard-jvm-metrics.json");
        addFileToContainer(getPrometheusConfig().getBytes(), "/otel-lgtm/prometheus.yaml");

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
        int httpPort = runtimeConfig.getOptionalValue("quarkus.http.port", Integer.class).orElse(0);

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
}
