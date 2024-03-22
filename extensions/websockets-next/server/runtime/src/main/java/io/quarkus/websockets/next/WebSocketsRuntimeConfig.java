package io.quarkus.websockets.next;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.websockets-next")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface WebSocketsRuntimeConfig {

    /**
     * See <a href="https://datatracker.ietf.org/doc/html/rfc6455#page-12">The WebSocket Protocol</a>
     *
     * @return the supported subprotocols
     */
    Optional<List<String>> supportedSubprotocols();

    /**
     * TODO Not implemented yet.
     *
     * The default timeout to complete processing of a message.
     */
    Optional<Duration> timeout();

}
