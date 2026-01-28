package io.quarkus.aesh.runtime;

import java.time.Instant;

/**
 * CDI event payload for aesh session lifecycle events.
 * <p>
 * Fired asynchronously with {@link SessionOpened} or {@link SessionClosed} qualifiers
 * when remote terminal sessions are opened or closed.
 *
 * @see SessionOpened
 * @see SessionClosed
 */
public class AeshSessionEvent {

    private final String sessionId;
    private final String transport;
    private final Instant timestamp;

    public AeshSessionEvent(String sessionId, String transport, Instant timestamp) {
        this.sessionId = sessionId;
        this.transport = transport;
        this.timestamp = timestamp;
    }

    /**
     * Unique identifier for this session.
     */
    public String sessionId() {
        return sessionId;
    }

    /**
     * Transport type: {@code "ssh"}, {@code "websocket"}, or {@code "unknown"}.
     */
    public String transport() {
        return transport;
    }

    /**
     * Timestamp when the event occurred.
     */
    public Instant timestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "AeshSessionEvent{sessionId='" + sessionId + "', transport='" + transport + "', timestamp=" + timestamp + "}";
    }
}
