package io.quarkus.websockets.next;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

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
     * The interval after which, when set, the client sends a ping message to a connected server automatically.
     * <p>
     * Ping messages are not sent automatically by default.
     */
    Optional<Duration> autoPingInterval();

    /**
     * The strategy used when an error occurs but no error handler can handle the failure.
     * <p>
     * By default, the connection is closed when an unhandled failure occurs.
     */
    @WithDefault("close")
    UnhandledFailureStrategy unhandledFailureStrategy();

}
