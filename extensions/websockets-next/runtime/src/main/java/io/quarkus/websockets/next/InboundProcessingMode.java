package io.quarkus.websockets.next;

import io.smallrye.common.annotation.Experimental;

/**
 * WebSocket endpoints define the mode used to process incoming events for a specific connection.
 * <p>
 * An incoming event can represent a message (text, binary, pong), opening connection and closing connection.
 *
 * @see WebSocketConnection
 * @see WebSocketClientConnection
 */
@Experimental("This API is experimental and may change in the future")
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