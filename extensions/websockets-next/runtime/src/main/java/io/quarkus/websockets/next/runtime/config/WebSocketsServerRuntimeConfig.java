package io.quarkus.websockets.next.runtime.config;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.websockets-next.server")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface WebSocketsServerRuntimeConfig {

    /**
     * See <a href="https://datatracker.ietf.org/doc/html/rfc6455#page-12">The WebSocket Protocol</a>
     */
    Optional<List<String>> supportedSubprotocols();

    /**
     * Compression Extensions for WebSocket are supported by default.
     * <p>
     * See also <a href="https://datatracker.ietf.org/doc/html/rfc7692">RFC 7692</a>
     */
    @WithDefault("true")
    boolean perMessageCompressionSupported();

    /**
     * The compression level must be a value between 0 and 9. The default value is
     * {@value io.vertx.core.http.HttpServerOptions#DEFAULT_WEBSOCKET_COMPRESSION_LEVEL}.
     */
    OptionalInt compressionLevel();

    /**
     * The maximum size of a message in bytes. The default values is
     * {@value io.vertx.core.http.HttpServerOptions#DEFAULT_MAX_WEBSOCKET_MESSAGE_SIZE}.
     */
    OptionalInt maxMessageSize();

    /**
     * The maximum size of a frame in bytes. The default values is
     * {@value io.vertx.core.http.HttpServerOptions#DEFAULT_MAX_WEBSOCKET_FRAME_SIZE}.
     */
    OptionalInt maxFrameSize();

    /**
     * The interval after which, when set, the server sends a ping message to a connected client automatically.
     * <p>
     * Ping messages are not sent automatically by default.
     */
    Optional<Duration> autoPingInterval();

    /**
     * The strategy used when an error occurs but no error handler can handle the failure.
     * <p>
     * By default, the error message is logged and the connection is closed when an unhandled failure occurs.
     */
    @WithDefault("log-and-close")
    UnhandledFailureStrategy unhandledFailureStrategy();

    /**
     * WebSockets-specific security configuration.
     */
    Security security();

    /**
     * Dev mode configuration.
     */
    DevMode devMode();

    /**
     * Traffic logging config.
     */
    TrafficLoggingConfig trafficLogging();

    /**
     * Telemetry configuration.
     */
    @WithParentName
    TelemetryConfig telemetry();

    interface Security {

        /**
         * Quarkus redirects HTTP handshake request to this URL if an HTTP upgrade is rejected due to the authorization
         * failure. This configuration property takes effect when you secure endpoint with a standard security annotation.
         * For example, the HTTP upgrade is secured if an endpoint class is annotated with the `@RolesAllowed` annotation.
         */
        Optional<String> authFailureRedirectUrl();

    }

    interface DevMode {

        /**
         * The limit of messages kept for a Dev UI connection. If less than zero then no messages are stored and sent to the Dev
         * UI view.
         */
        @WithDefault("1000")
        long connectionMessagesLimit();

    }

}
