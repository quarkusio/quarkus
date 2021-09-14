package io.quarkus.devservices.common;

public class ContainerAddress {
    private final String host;
    private final int port;

    public ContainerAddress(String host, int port) {
        this.host = host;
        this.port = port;
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
}
