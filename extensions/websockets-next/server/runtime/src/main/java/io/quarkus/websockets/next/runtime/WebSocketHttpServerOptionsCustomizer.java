package io.quarkus.websockets.next.runtime;

import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import io.quarkus.vertx.http.HttpServerOptionsCustomizer;
import io.quarkus.websockets.next.WebSocketsRuntimeConfig;
import io.vertx.core.http.HttpServerOptions;

@Dependent
public class WebSocketHttpServerOptionsCustomizer implements HttpServerOptionsCustomizer {

    @Inject
    WebSocketsRuntimeConfig config;

    @Override
    public void customizeHttpServer(HttpServerOptions options) {
        config.supportedSubprotocols().orElse(List.of()).forEach(options::addWebSocketSubProtocol);
    }

    @Override
    public void customizeHttpsServer(HttpServerOptions options) {
        config.supportedSubprotocols().orElse(List.of()).forEach(options::addWebSocketSubProtocol);
    }

}
