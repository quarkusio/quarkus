package io.quarkus.websockets.next.runtime;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.websockets.next.OpenClientConnections;
import io.quarkus.websockets.next.WebSocketClientConnection;

@Singleton
public class ClientConnectionManager implements OpenClientConnections {

    private static final Logger LOG = Logger.getLogger(ClientConnectionManager.class);

    private final ConcurrentMap<String, Set<WebSocketClientConnection>> endpointToConnections = new ConcurrentHashMap<>();

    private final List<ClientConnectionListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public Iterator<WebSocketClientConnection> iterator() {
        return stream().iterator();
    }

    @Override
    public Stream<WebSocketClientConnection> stream() {
        return endpointToConnections.values().stream().flatMap(Set::stream).filter(WebSocketClientConnection::isOpen);
    }

    void add(String endpoint, WebSocketClientConnection connection) {
        LOG.debugf("Add client connection: %s", connection);
        if (endpointToConnections.computeIfAbsent(endpoint, e -> ConcurrentHashMap.newKeySet()).add(connection)) {
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

    void remove(String endpoint, WebSocketClientConnection connection) {
        LOG.debugf("Remove client connection: %s", connection);
        Set<WebSocketClientConnection> connections = endpointToConnections.get(endpoint);
        if (connections != null) {
            if (connections.remove(connection)) {
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
    public Set<WebSocketClientConnection> getConnections(String endpoint) {
        Set<WebSocketClientConnection> ret = endpointToConnections.get(endpoint);
        if (ret == null) {
            return Set.of();
        }
        return ret;
    }

    public void addListener(ClientConnectionListener listener) {
        this.listeners.add(listener);
    }

    @PreDestroy
    void destroy() {
        endpointToConnections.clear();
    }

    public interface ClientConnectionListener {

        void connectionAdded(String endpoint, WebSocketClientConnection connection);

        void connectionRemoved(String endpoint, String connectionId);
    }

}
