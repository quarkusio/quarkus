package io.quarkus.websockets.next;

import java.util.Set;
import java.util.function.Predicate;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Uni;

/**
 * A WebSocket connection represents a client connected to a specific WebSocket endpoint.
 */
public interface WebSocketServerConnection extends Sender, BlockingSender {

    /**
     *
     * @return the unique identifier assigned to this connection
     */
    String id();

    /**
     *
     * @param name
     * @return the actual value of the path parameter or null
     */
    String pathParam(String name);

    /**
     * Sends messages to all open clients connected to the same WebSocket endpoint.
     *
     * @return the broadcast sender
     * @see #getOpenConnections()
     */
    BroadcastSender broadcast();

    /**
     * Sends messages to all open clients connected to the same WebSocket endpoint and matching the given filter predicate.
     *
     * @param filter
     * @return the broadcast sender
     * @see #getOpenConnections()
     */
    BroadcastSender broadcast(Predicate<WebSocketServerConnection> filter);

    /**
     * The returned set also includes the connection this method is called upon.
     *
     * @return the set of open connections to the same endpoint
     */
    Set<WebSocketServerConnection> getOpenConnections();

    /**
     * @return {@code true} if the HTTP connection is encrypted via SSL/TLS
     */
    boolean isSecure();

    /**
     * @return {@code true} if the WebSocket is closed
     */
    boolean isClosed();

    /**
     *
     * @return {@code true} if the WebSocket is open
     */
    default boolean isOpen() {
        return !isClosed();
    }

    /**
     *
     * @return a new {@link Uni} with a {@code null} item
     */
    @CheckReturnValue
    Uni<Void> close();

    /**
     * Makes it possible to send messages to all clients connected to the same WebSocket endpoint.
     *
     * @see WebSocketServerConnection#getOpenConnections()
     */
    interface BroadcastSender extends Sender, BlockingSender {

    }

}
