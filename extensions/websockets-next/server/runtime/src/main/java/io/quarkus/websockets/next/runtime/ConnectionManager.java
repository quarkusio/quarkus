package io.quarkus.websockets.next.runtime;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.websockets.next.WebSocketConnection;

@Singleton
public class ConnectionManager {

    private static final Logger LOG = Logger.getLogger(ConnectionManager.class);

    private final ConcurrentMap<String, Set<WebSocketConnection>> endpointToConnections = new ConcurrentHashMap<>();

    private final List<ConnectionListener> listeners = new CopyOnWriteArrayList<>();

    void add(String endpoint, WebSocketConnection connection) {
        LOG.debugf("Add connection: %s", connection);
        if (endpointToConnections.computeIfAbsent(endpoint, e -> ConcurrentHashMap.newKeySet()).add(connection)) {
            if (!listeners.isEmpty()) {
                for (ConnectionListener listener : listeners) {
                    listener.connectionAdded(endpoint, connection);
                }
            }
        }
    }

    void remove(String endpoint, WebSocketConnection connection) {
        LOG.debugf("Remove connection: %s", connection);
        Set<WebSocketConnection> connections = endpointToConnections.get(endpoint);
        if (connections != null) {
            if (connections.remove(connection)) {
                if (!listeners.isEmpty()) {
                    for (ConnectionListener listener : listeners) {
                        listener.connectionRemoved(endpoint, connection.id());
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
