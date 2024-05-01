package io.quarkus.websockets.next;

import io.smallrye.common.annotation.Experimental;

/**
 * Defines the mode used to process incoming messages for a specific connection.
 *
 * @see WebSocketConnection
 * @see WebSocketClientConnection
 */
@Experimental("This API is experimental and may change in the future")
public enum InboundProcessingMode {

    /**
     * Messages are processed serially, ordering is guaranteed.
     */
    SERIAL,

    /**
     * Messages are processed concurrently, there are no ordering guarantees.
     */
    CONCURRENT,

}