package io.quarkus.websockets.next.runtime;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.websockets.next.WebSocketConnection;

@Singleton
public class ConnectionManager {

    private static final Logger LOG = Logger.getLogger(ConnectionManager.class);

    private final ConcurrentMap<String, Set<WebSocketConnection>> endpointToConnections = new ConcurrentHashMap<>();

    void add(String endpoint, WebSocketConnection connection) {
        LOG.debugf("Add connection: %s", connection);
        endpointToConnections.computeIfAbsent(endpoint, e -> ConcurrentHashMap.newKeySet()).add(connection);
    }

    void remove(String endpoint, WebSocketConnection connection) {
        LOG.debugf("Remove connection: %s", connection);
        Set<WebSocketConnection> connections = endpointToConnections.get(endpoint);
        if (connections != null) {
            connections.remove(connection);
        }
    }

    Set<WebSocketConnection> getConnections(String endpoint) {
        return endpointToConnections.get(endpoint);
    }

    @PreDestroy
    void destroy() {
        endpointToConnections.clear();
    }

}
