package io.quarkus.observability.common.config;

import java.time.Duration;
import java.util.Optional;

public abstract class AbstractGrafanaConfig extends AbstractContainerConfig implements GrafanaConfig {

    private final String username;
    private final String password;
    private final Optional<Integer> grafanaPort;

    public AbstractGrafanaConfig(String imageName) {
        this(imageName, true, "admin", "admin", Optional.empty());
    }

    public AbstractGrafanaConfig(String imageName, boolean shared) {
        this(imageName, shared, "admin", "admin", Optional.empty());
    }

    public AbstractGrafanaConfig(String imageName, String username, String password, Optional<Integer> grafanaPort) {
        this(imageName, true, username, password, grafanaPort);
    }

    public AbstractGrafanaConfig(String imageName, boolean shared, String username, String password,
            Optional<Integer> grafanaPort) {
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
    public Optional<Integer> grafanaPort() {
        return grafanaPort;
    }

    @Override
    public Duration timeout() {
        return Duration.ofMinutes(3);
    }
}
