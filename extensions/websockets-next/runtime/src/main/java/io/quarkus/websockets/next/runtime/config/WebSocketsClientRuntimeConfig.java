package io.quarkus.websockets.next.runtime.config;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.websockets-next.client")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface WebSocketsClientRuntimeConfig {

    /**
     * Compression Extensions for WebSocket are supported by default.
     * <p>
     * See also <a href="https://datatracker.ietf.org/doc/html/rfc7692">RFC 7692</a>
     */
    @WithDefault("false")
    boolean offerPerMessageCompression();

    /**
     * The compression level must be a value between 0 and 9. The default value is
     * {@value io.vertx.core.http.HttpClientOptions#DEFAULT_WEBSOCKET_COMPRESSION_LEVEL}.
     */
    OptionalInt compressionLevel();

    /**
     * The maximum size of a message in bytes. The default values is
     * {@value io.vertx.core.http.HttpClientOptions#DEFAULT_MAX_WEBSOCKET_MESSAGE_SIZE}.
     */
    OptionalInt maxMessageSize();

    /**
     * The maximum size of a frame in bytes. The default values is
     * {@value io.vertx.core.http.HttpClientOptions#DEFAULT_MAX_WEBSOCKET_FRAME_SIZE}.
     */
    OptionalInt maxFrameSize();

    /**
     * The interval after which, when set, the client sends a ping message to a connected server automatically.
     * <p>
     * Ping messages are not sent automatically by default.
     */
    Optional<Duration> autoPingInterval();

    /**
     * If set then a connection will be closed if no data is received nor sent within the given timeout.
     */
    Optional<Duration> connectionIdleTimeout();

    /**
     * The amount of time a client will wait until it closes the TCP connection after sending a close frame.
     * Any value will be {@link Duration#toSeconds() converted to seconds} and limited to {@link Integer#MAX_VALUE}.
     * The default value is {@value io.vertx.core.http.HttpClientOptions#DEFAULT_WEBSOCKET_CLOSING_TIMEOUT}s
     *
     * @see io.vertx.core.http.WebSocketClientOptions#setClosingTimeout(int)
     */
    Optional<Duration> connectionClosingTimeout();

    /**
     * The strategy used when an error occurs but no error handler can handle the failure.
     * <p>
     * By default, the error message is logged when an unhandled failure occurs.
     * <p>
     * Note that clients should not close the WebSocket connection arbitrarily. See also RFC-6455
     * <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-7.3">section 7.3</a>.
     */
    @WithDefault("log")
    UnhandledFailureStrategy unhandledFailureStrategy();

    /**
     * The name of the TLS configuration to use.
     * <p>
     * If a name is configured, it uses the configuration from {@code quarkus.tls.<name>.*}
     * If a name is configured, but no TLS configuration is found with that name then an error will be thrown.
     * <p>
     * The default TLS configuration is <strong>not</strong> used by default.
     */
    Optional<String> tlsConfigurationName();

    /**
     * Traffic logging config.
     */
    TrafficLoggingConfig trafficLogging();

    /**
     * Telemetry configuration.
     */
    @WithParentName
    TelemetryConfig telemetry();

}
