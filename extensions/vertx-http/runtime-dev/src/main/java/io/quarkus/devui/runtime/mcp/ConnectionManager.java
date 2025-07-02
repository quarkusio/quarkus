package io.quarkus.devui.runtime.mcp;

import java.util.Base64;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;

class ConnectionManager implements Iterable<McpConnectionBase> {

    private final Vertx vertx;

    private final ResponseHandlers responseHandlers;

    private final ConcurrentMap<String, ConnectionTimerId> connections = new ConcurrentHashMap<>();

    ConnectionManager(Vertx vertx, ResponseHandlers responseHandlers, McpServerRuntimeConfig config) {
        this.vertx = vertx;
        this.responseHandlers = responseHandlers;
        long minConnectionIdleTimeout = config.connectionIdleTimeout().toMillis();
        if (minConnectionIdleTimeout > 0) {
            vertx.setPeriodic(minConnectionIdleTimeout / 2, new Handler<Long>() {
                @Override
                public void handle(Long event) {
                    connections.values().removeIf(ConnectionTimerId::isIdleTimeoutExpired);
                }
            });
        }
    }

    @Override
    public Iterator<McpConnectionBase> iterator() {
        return connections.values().stream().map(ConnectionTimerId::connection).iterator();
    }

    public boolean has(String id) {
        return connections.containsKey(id);
    }

    public McpConnectionBase get(String id) {
        ConnectionTimerId connectionTimerId = connections.get(id);
        return connectionTimerId != null ? connectionTimerId.connection().touch() : null;
    }

    public void add(McpConnectionBase connection) {
        Long timerId = null;
        if (connection.autoPingInterval().isPresent()) {
            timerId = vertx.setPeriodic(connection.autoPingInterval().get().toMillis(), new Handler<Long>() {
                @Override
                public void handle(Long timerId) {
                    connection.send(Messages.newPing(responseHandlers.nextId()));
                }
            });
        }
        connections.put(connection.id(), new ConnectionTimerId(connection, timerId));

    }

    public boolean remove(String id) {
        ConnectionTimerId connection = connections.remove(id);
        if (connection != null) {
            connection.connection().close();
            if (connection.timerId() != null) {
                vertx.cancelTimer(connection.timerId());
            }
            return true;
        }
        return false;
    }

    record ConnectionTimerId(McpConnectionBase connection, Long timerId) {

        boolean isIdleTimeoutExpired() {
            return connection.isIdleTimeoutExpired();
        }
    }

    public static String connectionId() {
        return Base64.getUrlEncoder().encodeToString(UUID.randomUUID().toString().getBytes());
    }

}
