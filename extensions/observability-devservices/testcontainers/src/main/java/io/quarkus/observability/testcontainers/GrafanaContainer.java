package io.quarkus.observability.testcontainers;

import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
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
        return new HttpWaitStrategy()
                .forPath("/")
                .forPort(config.grafanaPort())
                .forStatusCode(200)
                .withStartupTimeout(config.timeout());
    }
}
