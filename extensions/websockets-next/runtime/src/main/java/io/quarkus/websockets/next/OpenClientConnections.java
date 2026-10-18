package io.quarkus.websockets.next;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.inject.Default;

/**
 * Provides convenient access to all open client connections.
 * <p>
 * Quarkus provides a CDI bean with bean type {@link OpenClientConnections} and qualifier {@link Default}.
 */
public interface OpenClientConnections extends Iterable<WebSocketClientConnection> {

    /**
     * Returns an immutable snapshot of all open connections at the given time.
     *
     * @return an immutable collection of all open connections
     */
    default Collection<WebSocketClientConnection> listAll() {
        return stream().toList();
    }

    /**
     * Returns an immutable snapshot of all open connections for the given client id.
     *
     * @param endpointId
     * @return an immutable collection of all open connections for the given client id
     * @see WebSocketClient#clientId()
     */
    default Collection<WebSocketClientConnection> findByClientId(String clientId) {
        return stream().filter(c -> c.clientId().equals(clientId)).toList();
    }

    /**
     * Returns the open connection with the given id.
     *
     * @param connectionId
     * @return the open connection or empty {@link Optional} if no open connection with the given id exists
     * @see WebSocketConnection#id()
     */
    default Optional<WebSocketClientConnection> findByConnectionId(String connectionId) {
        return stream().filter(c -> c.id().equals(connectionId)).findFirst();
    }

    /**
     * Returns the stream of all open connections at the given time.
     *
     * @return the stream of open connections
     */
    Stream<WebSocketClientConnection> stream();

}
