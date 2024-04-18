package io.quarkus.websockets.next;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.vertx.core.http.HttpServerOptions;

@ConfigMapping(prefix = "quarkus.websockets-next")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface WebSocketsRuntimeConfig {

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
     * {@value HttpServerOptions#DEFAULT_WEBSOCKET_COMPRESSION_LEVEL}.
     */
    OptionalInt compressionLevel();

    /**
     * The maximum size of a message in bytes. The default values is
     * {@value HttpServerOptions#DEFAULT_MAX_WEBSOCKET_MESSAGE_SIZE}.
     */
    OptionalInt maxMessageSize();

}
