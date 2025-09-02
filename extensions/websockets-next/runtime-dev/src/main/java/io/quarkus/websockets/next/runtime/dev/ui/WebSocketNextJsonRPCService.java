package io.quarkus.websockets.next.runtime.dev.ui;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

import org.jboss.logging.Logger;

import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.runtime.ConnectionManager;
import io.quarkus.websockets.next.runtime.ConnectionManager.ConnectionListener;
import io.quarkus.websockets.next.runtime.config.WebSocketsServerRuntimeConfig;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class WebSocketNextJsonRPCService implements ConnectionListener {

    private static final Logger LOG = Logger.getLogger(WebSocketNextJsonRPCService.class);

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss");

    private static final String DEVUI_SOCKET_KEY_HEADER = "X-devui-socket-key";

    private final BroadcastProcessor<JsonObject> connectionStatus;
    private final BroadcastProcessor<JsonObject> connectionMessages;

    private final ConnectionManager connectionManager;

    private final Vertx vertx;

    private final ConcurrentMap<String, DevWebSocket> sockets;

    private final VertxHttpConfig httpConfig;

    private final WebSocketsServerRuntimeConfig.DevMode devModeConfig;

    WebSocketNextJsonRPCService(Instance<ConnectionManager> connectionManager, Vertx vertx, VertxHttpConfig httpConfig,
            WebSocketsServerRuntimeConfig config) {
        this.connectionStatus = BroadcastProcessor.create();
        this.connectionMessages = BroadcastProcessor.create();
        this.connectionManager = connectionManager.isResolvable() ? connectionManager.get() : null;
        this.vertx = vertx;
        this.httpConfig = httpConfig;
        this.devModeConfig = config.devMode();
        this.sockets = new ConcurrentHashMap<>();
        if (this.connectionManager != null) {
            this.connectionManager.addListener(this);
        }
    }

    public Multi<JsonObject> connectionStatus() {
        return connectionStatus;
    }

    public Multi<JsonObject> connectionMessages() {
        return connectionMessages;
    }

    public JsonObject getConnections(List<String> endpoints) {
        JsonObject json = new JsonObject();
        if (connectionManager != null) {
            for (String endpoint : endpoints) {
                List<WebSocketConnection> connections = new ArrayList<>(connectionManager.getConnections(endpoint));
                connections.sort(Comparator.comparing(WebSocketConnection::creationTime));
                JsonArray array = new JsonArray();
                for (WebSocketConnection c : connections) {
                    array.add(toJsonObject(endpoint, c));
                }
                json.put(endpoint, array);
            }
        }
        json.put("connectionMessagesLimit", devModeConfig.connectionMessagesLimit());
        return json;
    }

    public JsonArray getMessages(String connectionKey) {
        DevWebSocket socket = sockets.get(connectionKey);
        if (socket != null) {
            JsonArray ret = new JsonArray();
            List<TextMessage> messages = socket.messages;
            synchronized (messages) {
                for (ListIterator<TextMessage> it = messages.listIterator(messages.size()); it.hasPrevious();) {
                    ret.add(it.previous().toJsonObject());
                }
            }
            return ret;
        }
        return new JsonArray();
    }

    public Uni<JsonObject> openDevConnection(String path, String endpointPath) {
        if (connectionManager == null) {
            return failureUni();
        }
        if (isInvalidPath(path, endpointPath)) {
            LOG.errorf("Invalid path %s; original endpoint path %s", path, endpointPath);
            return failureUni();
        }
        WebSocketClient client = vertx.createWebSocketClient();
        String connectionKey = UUID.randomUUID().toString();
        Uni<WebSocket> uni = Uni.createFrom().completionStage(() -> client
                .connect(new WebSocketConnectOptions()
                        .setPort(httpConfig.port())
                        .setHost(httpConfig.host())
                        .setURI(path)
                        .addHeader(DEVUI_SOCKET_KEY_HEADER, connectionKey))
                .toCompletionStage());
        return uni.onItem().transform(s -> {
            LOG.debugf("Opened Dev UI connection with key %s to %s", connectionKey, path);
            List<TextMessage> messages = new ArrayList<>();
            s.textMessageHandler(m -> {
                synchronized (messages) {
                    if (messages.size() < devModeConfig.connectionMessagesLimit()) {
                        TextMessage t = new TextMessage(true, m, LocalDateTime.now());
                        messages.add(t);
                        connectionMessages.onNext(t.toJsonObject().put("key", connectionKey));
                    } else {
                        LOG.debugf("Opened Dev UI connection [%s] received a message but the limit [%s] has been reached",
                                connectionKey, devModeConfig.connectionMessagesLimit());
                    }
                }
            });
            sockets.put(connectionKey, new DevWebSocket(s, messages));
            return new JsonObject().put("success", true).put("key", connectionKey);
        }).onFailure().recoverWithItem(t -> {
            LOG.errorf(t, "Unable to open Dev UI connection with key %s to %s", connectionKey, path);
            return new JsonObject().put("success", false);
        });
    }

    static boolean isInvalidPath(String path, String endpointPath) {
        if (!endpointPath.contains("{")) {
            return !normalize(path).equals(endpointPath);
        }
        // "/foo/{bar}-1/baz" -> ["foo","{bar}","baz"]
        String[] endpointPathSegments = endpointPath.split("/");
        String[] pathSegments = normalize(path).split("/");
        if (endpointPathSegments.length != pathSegments.length) {
            return true;
        }
        for (int i = 0; i < endpointPathSegments.length; i++) {
            String es = endpointPathSegments[i];
            String s = pathSegments[i];
            if (es.startsWith("{") && es.endsWith("}")) {
                // path segment only contains path param
                continue;
            } else if (es.contains("{")) {
                String[] parts = es.split("\\{[a-zA-Z0-9_]+\\}");
                for (String part : parts) {
                    if (!s.contains(part)) {
                        return true;
                    }
                }
            } else if (!es.equals(s)) {
                // no path param and segments are not equal
                return true;
            }
        }
        return false;
    }

    private static String normalize(String path) {
        int queryIdx = path.indexOf("?");
        if (queryIdx != -1) {
            return path.substring(0, queryIdx);
        }
        return path;
    }

    public Uni<JsonObject> closeDevConnection(String connectionKey) {
        if (connectionManager == null) {
            return failureUni();
        }
        DevWebSocket socket = sockets.remove(connectionKey);
        if (socket != null) {
            Uni<Void> uni = Uni.createFrom().completionStage(() -> socket.socket.close().toCompletionStage());
            return uni.onItem().transform(v -> {
                LOG.debugf("Closed Dev UI connection with key %s", connectionKey);
                return new JsonObject().put("success", true);
            }).onFailure().recoverWithItem(t -> {
                LOG.errorf(t, "Unable to close Dev UI connection with key %s", connectionKey);
                return new JsonObject().put("success", false);
            });
        }
        return failureUni();
    }

    public Uni<JsonObject> sendTextMessage(String connectionKey, String message) {
        DevWebSocket socket = sockets.get(connectionKey);
        if (socket != null) {
            Uni<Void> uni = Uni.createFrom().completionStage(() -> socket.socket.writeTextMessage(message).toCompletionStage());
            return uni.onItem().transform(v -> {
                List<TextMessage> messages = socket.messages;
                synchronized (messages) {
                    if (messages.size() < devModeConfig.connectionMessagesLimit()) {
                        TextMessage t = new TextMessage(false, message, LocalDateTime.now());
                        messages.add(t);
                        connectionMessages.onNext(t.toJsonObject().put("key", connectionKey));
                        LOG.debugf("Sent text message to connection with key %s", connectionKey);
                    } else {
                        LOG.debugf("Sent text message to connection [%s] but the limit [%s] has been reached",
                                connectionKey, devModeConfig.connectionMessagesLimit());
                    }
                }
                return new JsonObject().put("success", true);
            }).onFailure().recoverWithItem(t -> {
                LOG.errorf(t, "Unable to send text message to connection with key %s", connectionKey);
                return new JsonObject().put("success", false);
            });
        }
        return failureUni();
    }

    public JsonObject clearMessages(String connectionKey) {
        DevWebSocket socket = sockets.get(connectionKey);
        if (socket != null) {
            socket.clearMessages();
            return new JsonObject().put("success", true);
        }
        return new JsonObject().put("success", false);
    }

    private Uni<JsonObject> failureUni() {
        return Uni.createFrom().item(new JsonObject().put("success", false));
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
        String key = c.handshakeRequest().header(DEVUI_SOCKET_KEY_HEADER);
        if (key != null) {
            json.put("devuiSocketKey", key);
        }
        return json;
    }

    record DevWebSocket(WebSocket socket, List<TextMessage> messages) {

        void clearMessages() {
            synchronized (messages) {
                messages.clear();
            }
        }
    }

    record TextMessage(boolean incoming, String text, LocalDateTime timestamp) {

        JsonObject toJsonObject() {
            return new JsonObject()
                    .put("text", text)
                    .put("incoming", incoming)
                    .put("time", timestamp.format(FORMATTER))
                    .put("className", incoming ? "incoming" : "outgoing")
                    .put("userAbbr", incoming ? "IN" : "OUT");
        }

    }

}
