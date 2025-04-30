package io.quarkus.observability.common.config;

import java.time.Duration;

import io.quarkus.observability.common.ContainerConstants;

public abstract class AbstractGrafanaConfig extends AbstractContainerConfig implements GrafanaConfig {

    private final String username;
    private final String password;
    private final int grafanaPort;

    public AbstractGrafanaConfig(String imageName) {
        this(imageName, true, "admin", "admin", ContainerConstants.GRAFANA_PORT);
    }

    public AbstractGrafanaConfig(String imageName, boolean shared) {
        this(imageName, shared, "admin", "admin", ContainerConstants.GRAFANA_PORT);
    }

    public AbstractGrafanaConfig(String imageName, String username, String password, int grafanaPort) {
        this(imageName, true, username, password, grafanaPort);
    }

    public AbstractGrafanaConfig(String imageName, boolean shared, String username, String password, int grafanaPort) {
        super(imageName, shared);
        this.username = username;
        this.password = password;
        this.grafanaPort = grafanaPort;
    }

    @Override
    public String username() {
        return username;
    }

    @Override
    public String password() {
        return password;
    }

    @Override
    public int grafanaPort() {
        return grafanaPort;
    }

    @Override
    public Duration timeout() {
        return Duration.ofMinutes(3);
    }
}
