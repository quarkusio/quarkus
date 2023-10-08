package io.quarkus.devservices.common;

import io.quarkus.deployment.dev.devservices.RunningContainer;

public class ContainerAddress {
    private final String id;
    private final String host;
    private final int port;
    private final RunningContainer runningContainer;

    public ContainerAddress(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.runningContainer = null;
    }

    public ContainerAddress(RunningContainer runningContainer, String host, int port) {
        this.runningContainer = runningContainer;
        this.id = runningContainer.containerInfo().id();
        this.host = host;
        this.port = port;
    }

    public String getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUrl() {
        return String.format("%s:%d", host, port);
    }

    public RunningContainer getRunningContainer() {
        return runningContainer;
    }
}
