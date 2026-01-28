package io.quarkus.aesh.runtime;

/**
 * Shared interface for remote transport session data.
 * <p>
 * Implemented by transport-specific beans ({@code AeshWebSocketEndpoint},
 * {@code SshServerLifecycle}) to expose session information to the Dev UI
 * without creating hard dependencies from the core module on sub-modules.
 */
public interface TransportSessionInfo {

    /**
     * Transport name, e.g. {@code "ssh"} or {@code "websocket"}.
     */
    String getTransportName();

    /**
     * Number of currently active sessions on this transport.
     */
    int getActiveSessionCount();

    /**
     * Configured maximum sessions, or {@code -1} if unlimited.
     */
    int getMaxSessions();

    /**
     * Whether this transport is currently running and accepting connections.
     */
    boolean isRunning();
}
