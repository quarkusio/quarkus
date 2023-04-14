package io.quarkus.observability.testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import io.quarkus.observability.common.ContainerConstants;
import io.quarkus.observability.common.config.AbstractContainerConfig;
import io.quarkus.observability.common.config.ConfigUtils;
import io.quarkus.observability.common.config.GrafanaConfig;
import io.quarkus.observability.common.config.ModulesConfiguration;
import io.quarkus.observability.common.config.VictoriaMetricsConfig;

@SuppressWarnings("resource")
public class GrafanaContainer extends ObservabilityContainer<GrafanaContainer, GrafanaConfig> {
    protected static final String GRAFANA_NETWORK_ALIAS = "grafana.testcontainer.docker";
    protected static final String DATASOURCES_PATH = "/etc/grafana/provisioning/datasources/custom.yaml";

    private final GrafanaConfig config;
    private final ModulesConfiguration root;

    // TODO -- configure?
    private String username = "admin";
    private String password = "password";
    private int port = 3000;

    public GrafanaContainer() {
        this(new GrafanaConfigImpl(), null);
    }

    public GrafanaContainer(GrafanaConfig config, ModulesConfiguration root) {
        super(config);
        this.config = config;
        this.root = root;
        withEnv("GF_SECURITY_ADMIN_USER", username);
        withEnv("GF_SECURITY_ADMIN_PASSWORD", password);
        withExposedPorts(port);
        waitingFor(grafanaWaitStrategy());
    }

    protected WaitStrategy grafanaWaitStrategy() {
        return new HttpWaitStrategy()
                .forPath("/")
                .forPort(port)
                .forStatusCode(200);
    }

    @Override
    protected void containerIsCreated(String containerId) {
        super.containerIsCreated(containerId);
        byte[] datasources = getResourceAsBytes(config.datasourcesFile());
        String content = new String(datasources, StandardCharsets.UTF_8);
        String vmEndpoint = "victoria-metrics:8428";
        if (root != null) {
            VictoriaMetricsConfig vmc = root.victoriaMetrics();
            vmEndpoint = ConfigUtils.vmEndpoint(vmc);
        }
        content = content.replace("xTARGETx", vmEndpoint);
        addFileToContainer(content.getBytes(StandardCharsets.UTF_8), DATASOURCES_PATH);
    }

    private static class GrafanaConfigImpl extends AbstractContainerConfig implements GrafanaConfig {
        public GrafanaConfigImpl() {
            super(ContainerConstants.GRAFANA);
        }

        @Override
        public Optional<Set<String>> networkAliases() {
            return Optional.of(Set.of("grafana", GRAFANA_NETWORK_ALIAS));
        }

        @Override
        public String datasourcesFile() {
            return "datasources.yaml";
        }
    }
}
