package io.quarkus.devservices.common;

public class ContainerAddress {
    private final String id;
    private final String host;
    private final int port;

    public ContainerAddress(String id, String host, int port) {
        this.id = id;
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
}
