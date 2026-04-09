package io.quarkus.vertx.http.runtime;

import java.util.Optional;

import io.smallrye.config.WithDefault;

/**
 * Configure the Vert.X HTTP Server for WebSocket Server connection.
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

    /**
     * Enable per-frame WebSocket compression.
     */
    @WithDefault("true")
    boolean perFrameCompression();

    /**
     * Enable per-message WebSocket compression.
     */
    @WithDefault("true")
    boolean perMessageCompression();

    /**
     * WebSocket compression level (1-9).
     */
    @WithDefault("6")
    int compressionLevel();

    /**
     * Allow server to use no-context takeover for WebSocket compression.
     */
    @WithDefault("false")
    boolean allowServerNoContext();

    /**
     * Prefer client to use no-context takeover for WebSocket compression.
     */
    @WithDefault("false")
    boolean preferredClientNoContext();

    /**
     * WebSocket close handshake timeout in seconds.
     */
    @WithDefault("10")
    int closingTimeout();

    /**
     * Accept WebSocket frames without masking.
     * Should only be used for testing.
     */
    @WithDefault("false")
    boolean acceptUnmaskedFrames();
}
