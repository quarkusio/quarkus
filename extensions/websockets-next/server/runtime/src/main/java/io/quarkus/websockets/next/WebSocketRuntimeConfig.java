package io.quarkus.websockets.next;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.web-socket-next")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface WebSocketRuntimeConfig {

    /**
     * TODO Not implemented yet.
     *
     * The default timeout to complete processing of a message.
     */
    Optional<Duration> timeout();

}
