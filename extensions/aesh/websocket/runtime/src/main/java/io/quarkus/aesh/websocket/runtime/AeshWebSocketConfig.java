package io.quarkus.aesh.websocket.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configuration for the Aesh WebSocket terminal extension.
 */
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
@ConfigMapping(prefix = "quarkus.aesh.websocket")
public interface AeshWebSocketConfig {

    /**
     * Whether the WebSocket terminal endpoint is enabled.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Comma-separated list of roles required to access the WebSocket terminal.
     * Requires a Quarkus Security extension (e.g. quarkus-elytron-security-properties-file).
     */
    Optional<List<String>> rolesAllowed();

    /**
     * Whether the WebSocket terminal requires an authenticated user (any role).
     * Ignored if roles-allowed is set. Requires a Quarkus Security extension.
     */
    @WithDefault("false")
    boolean authenticated();

    /**
     * The path for the WebSocket terminal endpoint.
     */
    @WithDefault("/aesh/terminal")
    String path();

    /**
     * Whether the health check is published when the smallrye-health extension is present.
     */
    @WithName("health.enabled")
    @WithDefault("true")
    boolean healthEnabled();
}
