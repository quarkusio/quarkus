package io.quarkus.websockets.next.runtime;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.inject.Singleton;

import io.quarkus.websockets.next.WebSocketServerConnection;

@Singleton
public class ConnectionManager {

    private final ConcurrentMap<String, Set<WebSocketServerConnection>> endpointToConnections = new ConcurrentHashMap<>();

    void add(String endpoint, WebSocketServerConnection connection) {
        endpointToConnections.computeIfAbsent(endpoint, e -> ConcurrentHashMap.newKeySet()).add(connection);
    }

    void remove(String endpoint, WebSocketServerConnection connection) {
        Set<WebSocketServerConnection> connections = endpointToConnections.get(endpoint);
        if (connections != null) {
            connections.remove(connection);
        }
    }

    Set<WebSocketServerConnection> getConnections(String endpoint) {
        return endpointToConnections.get(endpoint);
    }

}
