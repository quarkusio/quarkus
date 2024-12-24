package io.quarkus.vertx.http.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

/**
 * Configure the Vert.X HTTP Server for WebSocker Server connection.
 */
@ConfigGroup
public interface WebsocketServerConfig {

    /**
     * The maximum amount of data that can be sent in a single frame.
     *
     * Messages larger than this must be broken up into continuation frames.
     *
     * Default 65536 (from HttpServerOptions of Vert.X HttpServerOptions)
     */
    Optional<Integer> maxFrameSize();

    /**
     * The maximum WebSocket message size.
     *
     * Default 262144 (from HttpServerOptions of Vert.X HttpServerOptions)
     */
    Optional<Integer> maxMessageSize();

}
