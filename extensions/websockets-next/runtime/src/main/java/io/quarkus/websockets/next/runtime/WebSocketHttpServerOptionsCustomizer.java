package io.quarkus.websockets.next.runtime;

import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import io.quarkus.vertx.http.HttpServerConfigCustomizer;
import io.quarkus.websockets.next.runtime.config.WebSocketsServerRuntimeConfig;
import io.vertx.core.http.HttpServerConfig;
import io.vertx.core.http.WebSocketServerConfig;
import io.vertx.core.net.ServerSSLOptions;

@Dependent
public class WebSocketHttpServerOptionsCustomizer implements HttpServerConfigCustomizer {

    @Inject
    WebSocketsServerRuntimeConfig config;

    @Override
    public void customizeHttpServer(HttpServerConfig httpConfig) {
        customize(httpConfig);
    }

    @Override
    public void customizeHttpsServer(HttpServerConfig httpConfig, ServerSSLOptions sslOptions) {
        customize(httpConfig);
    }

    private void customize(HttpServerConfig httpConfig) {
        WebSocketServerConfig wsConfig = httpConfig.getWebSocketConfig();
        if (wsConfig == null) {
            wsConfig = new WebSocketServerConfig();
        }
        List<String> subProtocols = config.supportedSubprotocols().orElse(List.of());
        if (!subProtocols.isEmpty()) {
            wsConfig.setSubProtocols(subProtocols);
        }
        wsConfig.setUsePerMessageCompression(config.perMessageCompressionSupported());
        if (config.compressionLevel().isPresent()) {
            wsConfig.setCompressionLevel(config.compressionLevel().getAsInt());
        }
        if (config.maxMessageSize().isPresent()) {
            wsConfig.setMaxMessageSize(config.maxMessageSize().getAsInt());
        }
        if (config.maxFrameSize().isPresent()) {
            wsConfig.setMaxFrameSize(config.maxFrameSize().getAsInt());
        }
        httpConfig.setWebSocketConfig(wsConfig);
    }

}
