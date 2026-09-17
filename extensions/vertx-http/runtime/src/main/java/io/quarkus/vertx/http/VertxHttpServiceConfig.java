package io.quarkus.vertx.http;

import java.util.Map;

import io.smallrye.stork.api.config.ConfigWithType;

public class VertxHttpServiceConfig implements ConfigWithType {

    private final HttpServer httpServer;
    private final String domainSocket;

    public VertxHttpServiceConfig(final HttpServer httpServer) {
        this(httpServer, null);
    }

    public VertxHttpServiceConfig(final HttpServer httpServer, final String domainSocket) {
        this.httpServer = httpServer;
        this.domainSocket = domainSocket;
    }

    public HttpServer getHttpServer() {
        return httpServer;
    }

    public String getDomainSocket() {
        return domainSocket;
    }

    @Override
    public String type() {
        return "vertx-http";
    }

    @Override
    public Map<String, String> parameters() {
        return Map.of();
    }
}
