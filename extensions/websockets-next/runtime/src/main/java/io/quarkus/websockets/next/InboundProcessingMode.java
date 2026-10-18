package io.quarkus.websockets.next;

/**
 * WebSocket endpoints define the mode used to process incoming events for a specific connection.
 * <p>
 * An incoming event can represent a message (text, binary, pong), opening connection and closing connection.
 *
 * @see WebSocketConnection
 * @see WebSocketClientConnection
 */
public enum InboundProcessingMode {

    /**
     * Events are processed serially, ordering is guaranteed.
     */
    SERIAL,

    /**
     * Events are processed concurrently, there are no ordering guarantees.
     */
    CONCURRENT,

}
