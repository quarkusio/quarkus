package io.quarkus.vertx.http;

import io.vertx.core.http.HttpServerOptions;

/**
 * Interface exposed by beans willing to customizing the HTTP server options.
 * <p>
 * This interface is composed of three methods allowing the customization of the different servers: HTTP, HTTPS and
 * domain socket.
 * <p>
 * The passed {@link HttpServerOptions} must be customized in the body of the implementation. The default
 * implementations are no-op.
 */
public interface HttpServerOptionsCustomizer {

    /**
     * Allows customizing the HTTP server options.
     */
    default void customizeHttpServer(HttpServerOptions options) {
        // NO-OP
    }

    /**
     * Allows customizing the HTTPS server options.
     */
    default void customizeHttpsServer(HttpServerOptions options) {
        // NO-OP
    }

    /**
     * Allows customizing the server listening on a domain socket if any.
     */
    default void customizeDomainSocketServer(HttpServerOptions options) {
        // NO-OP
    }

}
