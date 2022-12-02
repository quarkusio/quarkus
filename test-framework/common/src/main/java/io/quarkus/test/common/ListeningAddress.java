package io.quarkus.test.common;

public class ListeningAddress {
    private final Integer port;
    private final String protocol;

    public ListeningAddress(Integer port, String protocol) {
        this.port = port;
        this.protocol = protocol;
    }

    public Integer getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public boolean isSsl() {
        return "https".equals(protocol);
    }
}
