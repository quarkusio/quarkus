package io.quarkus.vertx.http;

import io.vertx.core.http.HttpServerConfig;
import io.vertx.core.net.ServerSSLOptions;

/**
 * Interface exposed by CDI beans willing to customize the HTTP server configuration.
 * <p>
 * Replaces the deprecated {@link HttpServerOptionsCustomizer} to work with the new
 * Vert.x {@link HttpServerConfig} and {@link ServerSSLOptions} APIs.
 */
public interface HttpServerConfigCustomizer {

    /**
     * Allows customizing the HTTP server configuration.
     */
    default void customizeHttpServer(HttpServerConfig config) {
        // NO-OP
    }

    /**
     * Allows customizing the HTTPS server configuration.
     */
    default void customizeHttpsServer(HttpServerConfig config, ServerSSLOptions sslOptions) {
        // NO-OP
    }

    /**
     * Allows customizing the server listening on a domain socket if any.
     */
    default void customizeDomainSocketServer(HttpServerConfig config) {
        // NO-OP
    }

}
