package io.quarkus.devui.runtime.comms;

import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.MessageType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Arc;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcMethod;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcMethodName;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcReader;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcWriter;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.Cancellable;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;

/**
 * Route JsonRPC message to the correct method
 */
@ApplicationScoped
public class JsonRpcRouter {

    private final Map<Integer, Cancellable> subscriptions = new ConcurrentHashMap<>();

    // Map json-rpc method to java
    private final Map<String, ReflectionInfo> jsonRpcToJava = new HashMap<>();

    /**
     * This gets called on build to build into of the classes we are going to call in runtime
     *
     * @param extensionMethodsMap
     */
    public void populateJsonRPCMethods(Map<String, Map<JsonRpcMethodName, JsonRpcMethod>> extensionMethodsMap) {
        for (Map.Entry<String, Map<JsonRpcMethodName, JsonRpcMethod>> extension : extensionMethodsMap.entrySet()) {
            String extensionName = extension.getKey();
            Map<JsonRpcMethodName, JsonRpcMethod> jsonRpcMethods = extension.getValue();
            for (Map.Entry<JsonRpcMethodName, JsonRpcMethod> method : jsonRpcMethods.entrySet()) {
                JsonRpcMethodName methodName = method.getKey();
                JsonRpcMethod jsonRpcMethod = method.getValue();

                @SuppressWarnings("unchecked")
                Object providerInstance = Arc.container().select(jsonRpcMethod.getClazz()).get();

                try {
                    Method javaMethod;
                    Map<String, Class> params = null;
                    if (jsonRpcMethod.hasParams()) {
                        params = jsonRpcMethod.getParams();
                        javaMethod = providerInstance.getClass().getMethod(jsonRpcMethod.getMethodName(),
                                params.values().toArray(new Class[] {}));
                    } else {
                        javaMethod = providerInstance.getClass().getMethod(jsonRpcMethod.getMethodName());
                    }
                    ReflectionInfo reflectionInfo = new ReflectionInfo(jsonRpcMethod.getClazz(), providerInstance, javaMethod,
                            params);
                    String jsonRpcMethodName = extensionName + DOT + methodName;
                    jsonRpcToJava.put(jsonRpcMethodName, reflectionInfo);
                } catch (NoSuchMethodException | SecurityException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    public void addSocket(ServerWebSocket socket) {
        socket.textMessageHandler((e) -> {
            socket.writeTextMessage(route(e, socket));
        });
    }

    private String route(String message, ServerWebSocket s) {
        JsonRpcReader jsonRpcRequest = JsonRpcReader.read(message);
        JsonObject jsonRpcResponse = route(jsonRpcRequest, s);
        return jsonRpcResponse.encodePrettily();
    }

    @SuppressWarnings("unchecked")
    private JsonObject route(JsonRpcReader jsonRpcRequest, ServerWebSocket s) {

        String jsonRpcMethodName = jsonRpcRequest.getMethod();

        // First check some internal methods
        if (jsonRpcMethodName.equalsIgnoreCase(UNSUBSCRIBE)) {
            JsonObject jsonRpcResponse = JsonRpcWriter.writeResponse(jsonRpcRequest.getId(), null, MessageType.Void);

            if (this.subscriptions.containsKey(jsonRpcRequest.getId())) {
                Cancellable cancellable = this.subscriptions.remove(jsonRpcRequest.getId());
                cancellable.cancel();
            }
            return jsonRpcResponse;

        } else if (this.jsonRpcToJava.containsKey(jsonRpcMethodName)) { // Route to extension
            ReflectionInfo reflectionInfo = this.jsonRpcToJava.get(jsonRpcMethodName);
            Object providerInstance = Arc.container().select(reflectionInfo.bean).get();
            try {
                Object result;
                if (jsonRpcRequest.hasParams()) {
                    Object[] args = getArgsAsObjects(reflectionInfo.params, jsonRpcRequest);
                    result = reflectionInfo.method.invoke(providerInstance, args);
                } else {
                    result = reflectionInfo.method.invoke(providerInstance);
                }

                // Here wrap in our own object that contain some more metadata
                JsonObject jsonRpcResponse;
                if (reflectionInfo.isSubscription()) {
                    // Subscription
                    Multi<?> subscription = (Multi) result;

                    // TODO: If Jackson is on the classpath ?

                    Cancellable cancellable = subscription.subscribe().with((t) -> {
                        JsonObject jsonResponse = JsonRpcWriter.writeResponse(jsonRpcRequest.getId(), t,
                                MessageType.SubscriptionMessage);
                        s.writeTextMessage(jsonResponse.encodePrettily());
                    });

                    this.subscriptions.put(jsonRpcRequest.getId(), cancellable);

                    jsonRpcResponse = JsonRpcWriter.writeResponse(jsonRpcRequest.getId(), null, MessageType.Void);

                } else {
                    // Normal response
                    jsonRpcResponse = JsonRpcWriter.writeResponse(jsonRpcRequest.getId(), result, MessageType.Response);
                }

                return jsonRpcResponse;
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }

        // Method not found
        return JsonRpcWriter.writeMethodNotFoundResponse(jsonRpcRequest.getId(), jsonRpcMethodName);

    }

    private Object[] getArgsAsObjects(Map<String, Class> params, JsonRpcReader jsonRpcRequest) {
        List<Object> objects = new ArrayList<>();
        for (Map.Entry<String, Class> expectedParams : params.entrySet()) {
            String paramName = expectedParams.getKey();
            Class paramType = expectedParams.getValue();
            Object param = jsonRpcRequest.getParam(paramName);
            Object casted = paramType.cast(param);
            objects.add(casted);
        }
        return objects.toArray(Object[]::new);
    }

    private static final String DOT = ".";
    private static final String UNSUBSCRIBE = "unsubscribe";

}
