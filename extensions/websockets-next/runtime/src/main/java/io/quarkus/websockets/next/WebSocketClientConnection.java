package io.quarkus.websockets.next;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.common.annotation.Experimental;
import io.smallrye.mutiny.Uni;

/**
 * This interface represents a client connection to a WebSocket endpoint.
 * <p>
 * Quarkus provides a built-in CDI bean that implements this interface and can be injected in a {@link WebSocketClient}
 * endpoint and used to interact with the connected server.
 */
@Experimental("This API is experimental and may change in the future")
public interface WebSocketClientConnection extends Sender, BlockingSender {

    /**
     *
     * @return the unique identifier assigned to this connection
     */
    String id();

    /*
     * @return the client id
     */
    String clientId();

    /**
     *
     * @param name
     * @return the actual value of the path parameter or null
     * @see WebSocketClient#path()
     */
    String pathParam(String name);

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

}
