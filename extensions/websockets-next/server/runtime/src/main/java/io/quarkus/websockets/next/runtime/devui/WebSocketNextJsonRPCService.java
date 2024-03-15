package io.quarkus.websockets.next.runtime.devui;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.runtime.ConnectionManager;
import io.quarkus.websockets.next.runtime.ConnectionManager.ConnectionListener;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class WebSocketNextJsonRPCService implements ConnectionListener {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss");

    private final BroadcastProcessor<JsonObject> connectionStatus;
    private final ConnectionManager connectionManager;

    WebSocketNextJsonRPCService(ConnectionManager connectionManager) {
        this.connectionStatus = BroadcastProcessor.create();
        this.connectionManager = connectionManager;
        connectionManager.addListener(this);
    }

    public Multi<JsonObject> connectionStatus() {
        return connectionStatus;
    }

    public JsonObject getConnections(List<String> endpoints) {
        JsonObject json = new JsonObject();
        for (String endpoint : endpoints) {
            List<WebSocketConnection> connections = new ArrayList<>(connectionManager.getConnections(endpoint));
            connections.sort(Comparator.comparing(WebSocketConnection::creationTime));
            JsonArray array = new JsonArray();
            for (WebSocketConnection c : connections) {
                array.add(toJsonObject(endpoint, c));
            }
            json.put(endpoint, array);
        }
        return json;
    }

    @Override
    public void connectionAdded(String endpoint, WebSocketConnection connection) {
        connectionStatus.onNext(toJsonObject(endpoint, connection));
    }

    @Override
    public void connectionRemoved(String endpoint, String connectionId) {
        connectionStatus.onNext(new JsonObject().put("id", connectionId).put("endpoint", endpoint).put("removed", true));
    }

    JsonObject toJsonObject(String endpoint, WebSocketConnection c) {
        JsonObject json = new JsonObject();
        json.put("id", c.id());
        json.put("endpoint", endpoint);
        json.put("creationTime",
                LocalDateTime.ofInstant(c.creationTime(), ZoneId.systemDefault()).format(FORMATTER));
        json.put("handshakePath", c.handshakeRequest().path());
        return json;
    }

}
