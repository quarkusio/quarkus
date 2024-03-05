package io.quarkus.websockets.next;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.common.annotation.Experimental;
import io.smallrye.mutiny.Uni;

/**
 * This interface represents a connection from a client to a specific {@link WebSocket} endpoint on the server.
 * <p>
 * Quarkus provides a built-in CDI bean of type {@code WebSocketServerConnection} that can be injected in a {@link WebSocket}
 * endpoint and used to interact with the connected client, or all clients connected to the endpoint respectively
 * (broadcasting).
 * <p>
 * Specifically, it is possible to send messages using blocking and non-blocking methods, declared on
 * {@link BlockingSender} and {@link Sender} respectively.
 */
@Experimental("This API is experimental and may change in the future")
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
     * @see WebSocket#path()
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
     * Close the connection.
     *
     * @return a new {@link Uni} with a {@code null} item
     */
    @CheckReturnValue
    Uni<Void> close();

    /**
     * Close the connection.
     */
    default void closeAndAwait() {
        close().await().indefinitely();
    }

    /**
     *
     * @return the handshake request
     */
    HandshakeRequest handshakeRequest();

    /**
     * Makes it possible to send messages to all clients connected to the same WebSocket endpoint.
     *
     * @see WebSocketServerConnection#getOpenConnections()
     */
    interface BroadcastSender extends Sender, BlockingSender {

    }

    /**
     * Provides some useful information about the initial handshake request.
     */
    interface HandshakeRequest {

        /**
         * The name is case insensitive.
         *
         * @param name
         * @return the first header value for the given header name, or {@code null}
         */
        String header(String name);

        /**
         * The name is case insensitive.
         *
         * @param name
         * @return an immutable list of header values for the given header name, never {@code null}
         */
        List<String> headers(String name);

        /**
         * Returned header names are lower case.
         *
         * @return an immutable map of header names to header values
         */
        Map<String, List<String>> headers();

        /**
         *
         * @return the scheme
         */
        String scheme();

        /**
         *
         * @return the host
         */
        String host();

        /**
         *
         * @return the port
         */
        int port();

        /**
         *
         * @return the path
         */
        String path();

        /**
         *
         * @return the query string
         */
        String query();

    }

}
