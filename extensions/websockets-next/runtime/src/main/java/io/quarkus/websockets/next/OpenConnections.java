package io.quarkus.websockets.next;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.inject.Default;

/**
 * Provides convenient access to all open connections.
 * <p>
 * Quarkus provides a CDI bean with bean type {@link OpenConnections} and qualifier {@link Default}.
 */
public interface OpenConnections extends Iterable<WebSocketConnection> {

    /**
     * Returns an immutable snapshot of all open connections at the given time.
     *
     * @return an immutable collection of all open connections
     */
    default Collection<WebSocketConnection> listAll() {
        return stream().toList();
    }

    /**
     * Returns an immutable snapshot of all open connections for the given endpoint id.
     *
     * @param endpointId
     * @return an immutable collection of all open connections for the given endpoint id
     * @see WebSocket#endpointId()
     */
    default Collection<WebSocketConnection> findByEndpointId(String endpointId) {
        return stream().filter(c -> c.endpointId().equals(endpointId)).toList();
    }

    /**
     * Returns the open connection with the given id.
     *
     * @param connectionId
     * @return the open connection or empty {@link Optional} if no open connection with the given id exists
     * @see WebSocketConnection#id()
     */
    default Optional<WebSocketConnection> findByConnectionId(String connectionId) {
        return stream().filter(c -> c.id().equals(connectionId)).findFirst();
    }

    /**
     * Returns the stream of all open connections at the given time.
     *
     * @return the stream of open connections
     */
    Stream<WebSocketConnection> stream();

}
