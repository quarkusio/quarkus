package io.quarkus.vertx.http.runtime;

import java.util.Optional;

/**
 * Configure the Vert.X HTTP Server for WebSocker Server connection.
 */
public interface WebsocketServerConfig {
    /**
     * The maximum amount of data that can be sent in a single frame.
     * <p>
     * Messages larger than this must be broken up into continuation frames.
     * <p>
     * Default 65536 (from HttpServerOptions of Vert.X HttpServerOptions)
     */
    Optional<Integer> maxFrameSize();

    /**
     * The maximum WebSocket message size.
     * <p>
     * Default 262144 (from HttpServerOptions of Vert.X HttpServerOptions)
     */
    Optional<Integer> maxMessageSize();
}
