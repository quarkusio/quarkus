package io.quarkus.devshell.deployment;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketClientOptions;
import io.vertx.core.http.WebSocketConnectOptions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * WebSocket client for the Dev UI JSON-RPC endpoint.
 * Uses Jackson (not Vert.x JSON) to avoid classloading issues on the deployment CL.
 */
public class DevShellJsonRpcClient implements AutoCloseable {

    private static final Logger log = Logger.getLogger(DevShellJsonRpcClient.class);
    private static final int TIMEOUT_SECONDS = 10;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> LOCALHOST_ADDRESSES = Set.of(
            "localhost", "127.0.0.1", "::1", "0:0:0:0:0:0:0:1");

    private final URI uri;
    private final Vertx vertx;
    private final AtomicInteger idCounter = new AtomicInteger(1);
    private final ConcurrentHashMap<Integer, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Consumer<JsonNode>> subscriptions = new ConcurrentHashMap<>();

    private volatile WebSocketClient client;
    private volatile WebSocket socket;
    private volatile boolean connected;

    DevShellJsonRpcClient(String host, int port, String path) {
        this(host, port, path, List.of());
    }

    DevShellJsonRpcClient(String host, int port, String path, List<String> allowedHosts) {
        validateHost(host, allowedHosts);
        this.uri = URI.create("ws://" + host + ":" + port + path);
        this.vertx = Vertx.vertx();
    }

    private static void validateHost(String host, List<String> allowedHosts) {
        if (LOCALHOST_ADDRESSES.contains(host.toLowerCase())) {
            return;
        }
        if (allowedHosts != null && allowedHosts.contains(host)) {
            return;
        }
        try {
            InetAddress addr = InetAddress.getByName(host);
            if (addr.isLoopbackAddress()) {
                return;
            }
        } catch (Exception e) {
            // fall through to rejection
        }
        throw new SecurityException(
                "Dev Shell only allows localhost connections by default. "
                        + "To connect to '" + host + "', add it to quarkus.devshell.allowed-hosts");
    }

    public void connect() {
        WebSocketClientOptions options = new WebSocketClientOptions()
                .setDefaultHost(uri.getHost())
                .setDefaultPort(uri.getPort())
                .setMaxMessageSize(10 * 1024 * 1024);
        client = vertx.createWebSocketClient(options);

        try {
            WebSocketConnectOptions connectOptions = new WebSocketConnectOptions()
                    .setURI(uri.getPath());
            socket = client.connect(connectOptions).toCompletionStage().toCompletableFuture()
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            socket.textMessageHandler(this::handleResponse);

            socket.exceptionHandler(t -> log.error("WebSocket error", t));
            socket.closeHandler(v -> {
                connected = false;
                failAllPending("WebSocket closed");
            });

            connected = true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to Dev UI JSON-RPC at " + uri, e);
        }
    }

    public JsonNode call(String namespace, String methodName) {
        return call(namespace, methodName, MAPPER.createObjectNode());
    }

    public JsonNode call(String namespace, String methodName, ObjectNode params) {
        int id = idCounter.getAndIncrement();
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pendingRequests.put(id, future);

        try {
            ObjectNode request = MAPPER.createObjectNode()
                    .put("jsonrpc", "2.0")
                    .put("id", id)
                    .put("method", namespace + "_" + methodName);
            request.set("params", params);

            socket.writeTextMessage(MAPPER.writeValueAsString(request));
        } catch (Exception e) {
            pendingRequests.remove(id);
            throw new RuntimeException("Failed to send JSON-RPC request", e);
        }

        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            pendingRequests.remove(id);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException(cause.getMessage(), cause);
        }
    }

    public CompletableFuture<JsonNode> callAsync(String namespace, String methodName) {
        return callAsync(namespace, methodName, MAPPER.createObjectNode());
    }

    public CompletableFuture<JsonNode> callAsync(String namespace, String methodName, ObjectNode params) {
        int id = idCounter.getAndIncrement();
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pendingRequests.put(id, future);

        try {
            ObjectNode request = MAPPER.createObjectNode()
                    .put("jsonrpc", "2.0")
                    .put("id", id)
                    .put("method", namespace + "_" + methodName);
            request.set("params", params);

            socket.writeTextMessage(MAPPER.writeValueAsString(request));
        } catch (Exception e) {
            pendingRequests.remove(id);
            future.completeExceptionally(e);
        }
        return future;
    }

    public int subscribe(String namespace, String methodName, Consumer<JsonNode> onMessage) {
        return subscribe(namespace, methodName, MAPPER.createObjectNode(), onMessage);
    }

    public int subscribe(String namespace, String methodName, ObjectNode params, Consumer<JsonNode> onMessage) {
        int id = idCounter.getAndIncrement();
        subscriptions.put(id, onMessage);

        CompletableFuture<JsonNode> ackFuture = new CompletableFuture<>();
        pendingRequests.put(id, ackFuture);

        try {
            ObjectNode request = MAPPER.createObjectNode()
                    .put("jsonrpc", "2.0")
                    .put("id", id)
                    .put("method", namespace + "_" + methodName);
            request.set("params", params);

            socket.writeTextMessage(MAPPER.writeValueAsString(request));
        } catch (Exception e) {
            subscriptions.remove(id);
            pendingRequests.remove(id);
            throw new RuntimeException("Subscribe failed", e);
        }

        try {
            ackFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            subscriptions.remove(id);
            pendingRequests.remove(id);
            throw new RuntimeException("Subscribe failed: " + namespace + "_" + methodName, e);
        }
        return id;
    }

    public void unsubscribe(int subscriptionId) {
        subscriptions.remove(subscriptionId);
        if (connected && socket != null) {
            try {
                ObjectNode request = MAPPER.createObjectNode()
                        .put("jsonrpc", "2.0")
                        .put("id", subscriptionId)
                        .put("method", "unsubscribe");
                socket.writeTextMessage(MAPPER.writeValueAsString(request));
            } catch (Exception e) {
                log.debug("Failed to send unsubscribe", e);
            }
        }
    }

    public boolean isConnected() {
        return connected;
    }

    private void handleResponse(String text) {
        try {
            JsonNode json = MAPPER.readTree(text);
            int id = json.path("id").asInt(-1);

            if (json.has("error")) {
                CompletableFuture<JsonNode> future = pendingRequests.remove(id);
                if (future != null) {
                    JsonNode error = json.get("error");
                    String errorMsg = error.path("message").asText("Unknown error");
                    int idx = errorMsg.indexOf("failed:");
                    if (idx >= 0) {
                        errorMsg = errorMsg.substring(idx + 7).trim();
                    }
                    future.completeExceptionally(new RuntimeException(errorMsg));
                }
                return;
            }

            JsonNode result = json.get("result");
            if (result == null) {
                return;
            }

            String messageType = result.path("messageType").asText("Response");

            switch (messageType) {
                case "SubscriptionMessage":
                    Consumer<JsonNode> handler = subscriptions.get(id);
                    if (handler != null) {
                        handler.accept(result.path("object"));
                    }
                    break;
                case "Void":
                    CompletableFuture<JsonNode> ackFuture = pendingRequests.remove(id);
                    if (ackFuture != null) {
                        ackFuture.complete(MAPPER.createObjectNode());
                    }
                    break;
                default:
                    CompletableFuture<JsonNode> future = pendingRequests.remove(id);
                    if (future != null) {
                        future.complete(result.path("object"));
                    }
                    break;
            }
        } catch (Exception e) {
            log.error("Failed to parse JSON-RPC response", e);
        }
    }

    private void failAllPending(String reason) {
        RuntimeException ex = new RuntimeException(reason);
        pendingRequests.values().forEach(f -> f.completeExceptionally(ex));
        pendingRequests.clear();
    }

    @Override
    public void close() {
        connected = false;
        failAllPending("Client closed");
        subscriptions.clear();
        if (socket != null) {
            try {
                socket.close().toCompletionStage().toCompletableFuture().get(3, TimeUnit.SECONDS);
            } catch (Exception e) {
                // ignore
            }
        }
        if (client != null) {
            client.close();
        }
        if (vertx != null) {
            vertx.close();
        }
    }
}
