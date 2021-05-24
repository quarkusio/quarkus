package io.quarkus.it.smallrye.config;

import org.eclipse.microprofile.config.inject.ConfigProperties;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@ConfigProperties(prefix = "http.server")
public class ServerProperties {
    private String host;
    private int port;

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }
}
