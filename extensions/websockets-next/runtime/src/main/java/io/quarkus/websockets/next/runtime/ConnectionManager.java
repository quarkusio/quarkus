package io.quarkus.websockets.next.runtime;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.event.Event;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.websockets.next.Closed;
import io.quarkus.websockets.next.Open;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.WebSocketConnection;

@Singleton
public class ConnectionManager implements OpenConnections {

    private static final Logger LOG = Logger.getLogger(ConnectionManager.class);

    // generatedEndpointClassName -> open connections
    private final ConcurrentMap<String, Set<WebSocketConnection>> endpointToConnections = new ConcurrentHashMap<>();

    private final List<ConnectionListener> listeners = new CopyOnWriteArrayList<>();

    private final Event<WebSocketConnection> openEvent;
    private final Event<WebSocketConnection> closedEvent;

    ConnectionManager(@Open Event<WebSocketConnection> openEvent, @Closed Event<WebSocketConnection> closedEvent) {
        ArcContainer container = Arc.container();
        this.openEvent = container.resolveObserverMethods(WebSocketConnection.class, Open.Literal.INSTANCE).isEmpty()
                ? null
                : openEvent;
        this.closedEvent = container.resolveObserverMethods(WebSocketConnection.class, Closed.Literal.INSTANCE)
                .isEmpty() ? null : closedEvent;
    }

    @Override
    public Iterator<WebSocketConnection> iterator() {
        return stream().iterator();
    }

    @Override
    public Stream<WebSocketConnection> stream() {
        return endpointToConnections.values().stream().flatMap(Set::stream).filter(WebSocketConnection::isOpen);
    }

    void add(String endpoint, WebSocketConnection connection) {
        LOG.debugf("Add connection: %s", connection);
        if (endpointToConnections.computeIfAbsent(endpoint, e -> ConcurrentHashMap.newKeySet()).add(connection)) {
            if (openEvent != null) {
                openEvent.fireAsync(connection);
            }
            if (!listeners.isEmpty()) {
                for (ConnectionListener listener : listeners) {
                    try {
                        listener.connectionAdded(endpoint, connection);
                    } catch (Exception e) {
                        LOG.warnf("Unable to call listener#connectionAdded() on [%s]: %s", listener.getClass(),
                                e.toString());
                    }
                }
            }
        }
    }

    void remove(String endpoint, WebSocketConnection connection) {
        LOG.debugf("Remove connection: %s", connection);
        Set<WebSocketConnection> connections = endpointToConnections.get(endpoint);
        if (connections != null) {
            if (connections.remove(connection)) {
                if (closedEvent != null) {
                    closedEvent.fireAsync(connection);
                }
                if (!listeners.isEmpty()) {
                    for (ConnectionListener listener : listeners) {
                        try {
                            listener.connectionRemoved(endpoint, connection.id());
                        } catch (Exception e) {
                            LOG.warnf("Unable to call listener#connectionRemoved() on [%s]: %s", listener.getClass(),
                                    e.toString());
                        }
                    }
                }
            }
        }
    }

    /**
     *
     * @param endpoint
     * @return the connections for the given endpoint, never {@code null}
     */
    public Set<WebSocketConnection> getConnections(String endpoint) {
        Set<WebSocketConnection> ret = endpointToConnections.get(endpoint);
        if (ret == null) {
            return Set.of();
        }
        return ret;
    }

    public void addListener(ConnectionListener listener) {
        this.listeners.add(listener);
    }

    @PreDestroy
    void destroy() {
        endpointToConnections.clear();
    }

    public interface ConnectionListener {

        void connectionAdded(String endpoint, WebSocketConnection connection);

        void connectionRemoved(String endpoint, String connectionId);
    }

}
