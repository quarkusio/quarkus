package io.quarkus.aesh.runtime;

import java.time.Instant;
import java.util.Objects;

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

    /**
     * @param sessionId unique session identifier, must not be {@code null}
     * @param transport transport type (e.g. "ssh", "websocket"), must not be {@code null}
     * @param timestamp when the event occurred, must not be {@code null}
     */
    public AeshSessionEvent(String sessionId, String transport, Instant timestamp) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
        this.transport = Objects.requireNonNull(transport, "transport must not be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
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
