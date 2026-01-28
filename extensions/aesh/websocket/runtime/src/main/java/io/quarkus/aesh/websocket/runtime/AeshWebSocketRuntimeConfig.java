package io.quarkus.aesh.websocket.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

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
     * Zero means no limit.
     */
    @WithDefault("0")
    int maxConnections();

    /**
     * Idle timeout for WebSocket sessions. If a session has no input
     * activity for this duration, it will be closed. If not set,
     * sessions can remain idle indefinitely.
     */
    Optional<Duration> idleTimeout();
}
