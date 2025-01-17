package io.quarkus.observability.testcontainers;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import io.quarkus.observability.common.config.GrafanaConfig;

@SuppressWarnings("resource")
public abstract class GrafanaContainer<T extends GrafanaContainer<T, C>, C extends GrafanaConfig>
        extends ObservabilityContainer<T, C> {
    protected static final String DATASOURCES_PATH = "/etc/grafana/provisioning/datasources/custom.yaml";

    protected C config;

    public GrafanaContainer(C config) {
        super(config);
        this.config = config;
        withEnv("GF_SECURITY_ADMIN_USER", config.username());
        withEnv("GF_SECURITY_ADMIN_PASSWORD", config.password());
        addExposedPort(config.grafanaPort());
        waitingFor(grafanaWaitStrategy());
    }

    public int getGrafanaPort() {
        return getMappedPort(config.grafanaPort());
    }

    private WaitStrategy grafanaWaitStrategy() {
        return new WaitAllStrategy()
                .withStartupTimeout(config.timeout())
                .withStrategy(
                        Wait.forHttp("/")
                                .forPort(config.grafanaPort())
                                .forStatusCode(200)
                                .withStartupTimeout(config.timeout()))
                .withStrategy(
                        Wait.forLogMessage(".*The OpenTelemetry collector and the Grafana LGTM stack are up and running.*", 1)
                                .withStartupTimeout(config.timeout()));
    }
}
