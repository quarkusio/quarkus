package io.quarkus.observability.testcontainers;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import io.quarkus.observability.common.ContainerConstants;
import io.quarkus.observability.common.config.GrafanaConfig;

@SuppressWarnings("resource")
public abstract class GrafanaContainer<T extends GrafanaContainer<T, C>, C extends GrafanaConfig>
        extends ObservabilityContainer<T, C> {

    protected C config;

    public GrafanaContainer(C config) {
        super(config);
        this.config = config;
        withEnv("GF_SECURITY_ADMIN_USER", config.username());
        withEnv("GF_SECURITY_ADMIN_PASSWORD", config.password());
        addExposedPort(ContainerConstants.GRAFANA_PORT);
        config.grafanaPort().ifPresent(port -> addFixedExposedPort(port, ContainerConstants.GRAFANA_PORT));
        waitingFor(waitStrategy());
    }

    public int getGrafanaPort() {
        return getMappedPort(ContainerConstants.GRAFANA_PORT);
    }

    protected WaitStrategy waitStrategy() {
        return Wait.forHttp("/")
                .forPort(ContainerConstants.GRAFANA_PORT)
                .forStatusCode(200)
                .withStartupTimeout(config.timeout());
    }
}
