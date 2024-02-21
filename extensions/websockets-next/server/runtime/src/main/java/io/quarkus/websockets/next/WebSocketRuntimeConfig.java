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
     * The default timeout for callbacks that do not return {@link io.smallrye.mutiny.Multi}.
     */
    Optional<Duration> timeout();

}
