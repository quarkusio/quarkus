package io.quarkus.websockets.next;

import java.util.Set;
import java.util.function.Predicate;

import io.smallrye.common.annotation.Experimental;

/**
 * This interface represents a connection from a client to a specific {@link WebSocket} endpoint on the server.
 * <p>
 * Quarkus provides a built-in CDI bean that implements this interface and can be injected in a {@link WebSocket}
 * endpoint and used to interact with the connected client, or all clients connected to the endpoint respectively
 * (broadcasting).
 * <p>
 * Specifically, it is possible to send messages using blocking and non-blocking methods, declared on
 * {@link BlockingSender} and {@link Sender} respectively.
 */
@Experimental("This API is experimental and may change in the future")
public interface WebSocketConnection extends Connection {

    /**
     *
     * @return the endpoint id
     * @see WebSocket#endpointId()
     */
    String endpointId();

    /**
     * Sends messages to all open clients connected to the same WebSocket endpoint.
     *
     * @return the broadcast sender
     * @see #getOpenConnections()
     */
    BroadcastSender broadcast();

    /**
     * The returned set also includes the connection this method is called upon.
     *
     * @return the set of open connections to the same endpoint
     */
    Set<WebSocketConnection> getOpenConnections();

    /**
     *
     * @return the subprotocol selected by the handshake
     */
    String subprotocol();

    /**
     * Makes it possible to send messages to all clients connected to the same WebSocket endpoint.
     *
     * @see WebSocketConnection#getOpenConnections()
     */
    interface BroadcastSender extends BlockingSender {

        /**
         *
         * @param predicate
         * @return a new sender that sends messages to all open clients connected to the same WebSocket endpoint and matching
         *         the given filter predicate
         */
        BroadcastSender filter(Predicate<WebSocketConnection> predicate);

    }

}
