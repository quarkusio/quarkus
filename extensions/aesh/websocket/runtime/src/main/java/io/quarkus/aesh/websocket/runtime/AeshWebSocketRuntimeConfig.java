package io.quarkus.aesh.websocket.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Runtime configuration for the Aesh WebSocket terminal extension.
 * <p>
 * Build-time properties (enabled, security) are in {@link AeshWebSocketConfig}.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.aesh.websocket")
public interface AeshWebSocketRuntimeConfig {

    /**
     * Maximum number of concurrent WebSocket terminal sessions.
     * If not set or &lt;= 0, there is no limit.
     */
    OptionalInt maxConnections();

    /**
     * Idle timeout for WebSocket sessions. If a session has no input
     * activity for this duration, it will be closed. If not set,
     * sessions can remain idle indefinitely.
     */
    Optional<Duration> idleTimeout();
}
