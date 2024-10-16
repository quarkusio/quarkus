package io.quarkus.websockets.next;

import java.time.Instant;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Uni;

/**
 * WebSocket connection.
 *
 * @see WebSocketConnection
 * @see WebSocketClientConnection
 */
public interface Connection extends BlockingSender {

    /**
     *
     * @return the unique identifier assigned to this connection
     */
    String id();

    /**
     *
     * @param name
     * @return the value of the path parameter or {@code null}
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
     * @return the close reason or {@code null} if the connection is not closed
     */
    CloseReason closeReason();

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
    default Uni<Void> close() {
        return close(CloseReason.NORMAL);
    }

    /**
     * Close the connection with a specific reason.
     *
     * @param reason
     * @return a new {@link Uni} with a {@code null} item
     */
    Uni<Void> close(CloseReason reason);

    /**
     * Close the connection and wait for the completion.
     */
    default void closeAndAwait() {
        close().await().indefinitely();
    }

    /**
     * Close the connection with a specific reason and wait for the completion.
     */
    default void closeAndAwait(CloseReason reason) {
        close(reason).await().indefinitely();
    }

    /**
     *
     * @return the handshake request
     */
    HandshakeRequest handshakeRequest();

    /**
     *
     * @return the time when this connection was created
     */
    Instant creationTime();

    /**
     *
     * @return the user data associated with this connection
     */
    UserData userData();
}
