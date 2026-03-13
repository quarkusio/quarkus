package io.quarkus.devshell.runtime;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import io.quarkus.devui.runtime.comms.JsonRpcRouter;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcRequest;
import io.quarkus.devui.runtime.jsonrpc.json.JsonMapper;

/**
 * Bridge between Dev Shell TUI and the JsonRpcRouter.
 * Provides direct in-process method calls without any network overhead.
 */
public class DevShellRouter {

    private final JsonRpcRouter jsonRpcRouter;
    private final JsonMapper jsonMapper;
    private final AtomicInteger requestIdCounter = new AtomicInteger(0);
    private final Map<Integer, DevShellResponseWriter> activeSubscriptions = new ConcurrentHashMap<>();

    @Inject
    public DevShellRouter(JsonRpcRouter jsonRpcRouter) {
        this.jsonRpcRouter = jsonRpcRouter;
        jsonRpcRouter.getJsonRpcCodec();
        this.jsonMapper = jsonRpcRouter.getJsonMapper();
    }

    /**
     * Protected constructor for subclasses that handle routing differently (e.g., via reflection).
     */
    protected DevShellRouter() {
        this.jsonRpcRouter = null;
        this.jsonMapper = null;
    }

    /**
     * Call a JSON-RPC method and get the result as a CompletableFuture.
     *
     * @param method the method name (e.g., "devui-logstream.getLoggers")
     * @param params the parameters to pass to the method
     * @return a future that completes with the JSON response
     */
    public CompletableFuture<String> call(String method, Map<String, Object> params) {
        CompletableFuture<String> future = new CompletableFuture<>();

        JsonRpcRequest request = createRequest(method, params);
        DevShellResponseWriter writer = new DevShellResponseWriter(future);

        jsonRpcRouter.route(request, writer);

        return future;
    }

    /**
     * Call a JSON-RPC method and parse the result to the specified type.
     *
     * @param method the method name
     * @param params the parameters
     * @param resultType the expected result type
     * @return a future that completes with the parsed result
     */
    public <T> CompletableFuture<T> call(String method, Map<String, Object> params, Class<T> resultType) {
        return call(method, params)
                .thenApply(json -> parseResult(json, resultType));
    }

    /**
     * Subscribe to a JSON-RPC method that returns a stream (Multi).
     *
     * @param method the method name
     * @param params the parameters
     * @param onMessage callback for each message received
     * @param onError callback for errors
     * @param onComplete callback when the stream completes
     * @return the subscription ID that can be used to unsubscribe
     */
    public int subscribe(String method, Map<String, Object> params,
            Consumer<String> onMessage, Consumer<Throwable> onError, Runnable onComplete) {

        JsonRpcRequest request = createRequest(method, params);
        int subscriptionId = request.getId();

        DevShellResponseWriter writer = new DevShellResponseWriter(message -> {
            try {
                onMessage.accept(message);
            } catch (Exception e) {
                onError.accept(e);
            }
        });

        activeSubscriptions.put(subscriptionId, writer);
        jsonRpcRouter.route(request, writer);

        return subscriptionId;
    }

    /**
     * Unsubscribe from a streaming subscription.
     *
     * @param subscriptionId the subscription ID returned by subscribe()
     */
    public void unsubscribe(int subscriptionId) {
        DevShellResponseWriter writer = activeSubscriptions.remove(subscriptionId);
        if (writer != null) {
            writer.close();
        }

        // Send unsubscribe request to JsonRpcRouter
        JsonRpcRequest unsubscribeRequest = createRequest("unsubscribe", Map.of());
        unsubscribeRequest.setId(subscriptionId);
        jsonRpcRouter.route(unsubscribeRequest, new DevShellResponseWriter(response -> {
        }));
    }

    /**
     * Get all available runtime methods.
     */
    public Map<String, ?> getRuntimeMethods() {
        return jsonRpcRouter.getRuntimeMethodsMap();
    }

    /**
     * Get all available deployment methods.
     */
    public Map<String, ?> getDeploymentMethods() {
        return jsonRpcRouter.getDeploymentMethodsMap();
    }

    /**
     * Get the JsonMapper for serialization.
     */
    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    private JsonRpcRequest createRequest(String method, Map<String, Object> params) {
        JsonRpcRequest request = new JsonRpcRequest(jsonMapper);
        request.setId(requestIdCounter.incrementAndGet());
        request.setMethod(method);
        if (params != null && !params.isEmpty()) {
            request.setParams(params);
        }
        return request;
    }

    @SuppressWarnings("unchecked")
    private <T> T parseResult(String json, Class<T> resultType) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        // Parse the JSON-RPC response and extract the result
        try {
            Map<String, Object> response = jsonMapper.fromString(json, Map.class);
            Object result = response.get("result");
            if (result == null) {
                return null;
            }
            // If result contains "object", extract it (standard JSON-RPC response format)
            if (result instanceof Map) {
                Map<String, Object> resultMap = (Map<String, Object>) result;
                if (resultMap.containsKey("object")) {
                    result = resultMap.get("object");
                }
            }
            return jsonMapper.fromValue(result, resultType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON-RPC response", e);
        }
    }
}
