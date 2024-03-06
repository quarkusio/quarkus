package io.quarkus.websockets.next.runtime;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

import io.quarkus.websockets.next.WebSocketConnection;

@Singleton
public class ConnectionManager {

    private final ConcurrentMap<String, Set<WebSocketConnection>> endpointToConnections = new ConcurrentHashMap<>();

    void add(String endpoint, WebSocketConnection connection) {
        endpointToConnections.computeIfAbsent(endpoint, e -> ConcurrentHashMap.newKeySet()).add(connection);
    }

    void remove(String endpoint, WebSocketConnection connection) {
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
