package io.quarkus.websockets.next.runtime;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import jakarta.enterprise.event.Event;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.runtime.Shutdown;
import io.quarkus.websockets.next.Closed;
import io.quarkus.websockets.next.Open;
import io.quarkus.websockets.next.OpenClientConnections;
import io.quarkus.websockets.next.WebSocketClientConnection;

@Singleton
public class ClientConnectionManager implements OpenClientConnections {

    private static final Logger LOG = Logger.getLogger(ClientConnectionManager.class);

    private final ConcurrentMap<String, Set<WebSocketClientConnectionImpl>> endpointToConnections = new ConcurrentHashMap<>();

    private final List<ClientConnectionListener> listeners = new CopyOnWriteArrayList<>();

    private final Event<WebSocketClientConnection> openEvent;
    private final Event<WebSocketClientConnection> closedEvent;

    ClientConnectionManager(@Open Event<WebSocketClientConnection> openEvent,
            @Closed Event<WebSocketClientConnection> closedEvent) {
        ArcContainer container = Arc.container();
        this.openEvent = container.resolveObserverMethods(WebSocketClientConnection.class, Open.Literal.INSTANCE).isEmpty()
                ? null
                : openEvent;
        this.closedEvent = container.resolveObserverMethods(WebSocketClientConnection.class, Closed.Literal.INSTANCE)
                .isEmpty() ? null : closedEvent;
    }

    @Override
    public Iterator<WebSocketClientConnection> iterator() {
        return stream().iterator();
    }

    @Override
    public Stream<WebSocketClientConnection> stream() {
        return endpointToConnections.values().stream().flatMap(Set::stream).filter(WebSocketClientConnection::isOpen)
                .map(WebSocketClientConnection.class::cast);
    }

    void add(String endpoint, WebSocketClientConnectionImpl connection) {
        LOG.debugf("Add client connection: %s", connection);
        if (endpointToConnections.computeIfAbsent(endpoint, e -> ConcurrentHashMap.newKeySet()).add(connection)) {
            if (openEvent != null) {
                openEvent.fireAsync(connection);
            }
            if (!listeners.isEmpty()) {
                for (ClientConnectionListener listener : listeners) {
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

    void remove(String endpoint, WebSocketClientConnectionImpl connection) {
        LOG.debugf("Remove client connection: %s", connection);
        Set<WebSocketClientConnectionImpl> connections = endpointToConnections.get(endpoint);
        if (connections != null) {
            if (connections.remove(connection)) {
                if (closedEvent != null) {
                    closedEvent.fireAsync(connection);
                }
                if (!listeners.isEmpty()) {
                    for (ClientConnectionListener listener : listeners) {
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
     * @return the connections for the given client endpoint, never {@code null}
     */
    public Set<WebSocketClientConnectionImpl> getConnections(String endpoint) {
        Set<WebSocketClientConnectionImpl> ret = endpointToConnections.get(endpoint);
        if (ret == null) {
            return Set.of();
        }
        return ret;
    }

    public void addListener(ClientConnectionListener listener) {
        this.listeners.add(listener);
    }

    @Shutdown
    void cleanup() {
        if (!endpointToConnections.isEmpty()) {
            int sum = 0;
            for (Entry<String, Set<WebSocketClientConnectionImpl>> e : endpointToConnections.entrySet()) {
                for (WebSocketClientConnectionImpl c : e.getValue()) {
                    c.cleanup();
                    sum++;
                }
            }
            LOG.debugf("Cleanup performed for %s connections", sum);
            endpointToConnections.clear();
        }
    }

    public interface ClientConnectionListener {

        void connectionAdded(String endpoint, WebSocketClientConnection connection);

        void connectionRemoved(String endpoint, String connectionId);
    }

}
