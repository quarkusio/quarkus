package io.quarkus.devui.runtime.comms;

import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.MessageType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcMethod;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcMethodName;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcReader;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcWriter;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.unchecked.Unchecked;
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
                            params, jsonRpcMethod.getExplicitlyBlocking(), jsonRpcMethod.getExplicitlyNonBlocking());
                    String jsonRpcMethodName = extensionName + DOT + methodName;
                    jsonRpcToJava.put(jsonRpcMethodName, reflectionInfo);
                } catch (NoSuchMethodException | SecurityException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    private Uni<?> invoke(ReflectionInfo info, Object target, Object[] args) {
        if (info.isReturningUni()) {
            try {
                Uni<?> uni = ((Uni<?>) info.method.invoke(target, args));
                if (info.isExplicitlyBlocking()) {
                    return uni.runSubscriptionOn(Infrastructure.getDefaultExecutor());
                } else {
                    return uni;
                }
            } catch (Exception e) {
                return Uni.createFrom().failure(e);
            }
        } else {
            Uni<?> uni = Uni.createFrom().item(Unchecked.supplier(() -> info.method.invoke(target, args)));
            if (!info.isExplicitlyNonBlocking()) {
                return uni.runSubscriptionOn(Infrastructure.getDefaultExecutor());
            } else {
                return uni;
            }
        }
    }

    public void addSocket(ServerWebSocket socket) {
        socket.textMessageHandler((e) -> {
            JsonRpcReader jsonRpcRequest = JsonRpcReader.read(e);
            route(jsonRpcRequest, socket);
        });
    }

    @Inject
    Logger logger;

    @SuppressWarnings("unchecked")
    private void route(JsonRpcReader jsonRpcRequest, ServerWebSocket s) {
        String jsonRpcMethodName = jsonRpcRequest.getMethod();

        // First check some internal methods
        if (jsonRpcMethodName.equalsIgnoreCase(UNSUBSCRIBE)) {
            JsonObject jsonRpcResponse = JsonRpcWriter.writeResponse(jsonRpcRequest.getId(), null, MessageType.Void);

            if (this.subscriptions.containsKey(jsonRpcRequest.getId())) {
                Cancellable cancellable = this.subscriptions.remove(jsonRpcRequest.getId());
                cancellable.cancel();
            }
            s.writeTextMessage(jsonRpcResponse.encode());
        } else if (this.jsonRpcToJava.containsKey(jsonRpcMethodName)) { // Route to extension
            ReflectionInfo reflectionInfo = this.jsonRpcToJava.get(jsonRpcMethodName);
            Object target = Arc.container().select(reflectionInfo.bean).get();

            if (reflectionInfo.isReturningMulti()) {
                Multi<?> multi;
                try {
                    if (jsonRpcRequest.hasParams()) {
                        Object[] args = getArgsAsObjects(reflectionInfo.params, jsonRpcRequest);
                        multi = (Multi<?>) reflectionInfo.method.invoke(target, args);
                    } else {
                        multi = (Multi<?>) reflectionInfo.method.invoke(target);
                    }
                } catch (Exception e) {
                    logger.errorf(e, "Unable to invoke method %s using JSON-RPC, request was: %s", jsonRpcMethodName,
                            jsonRpcRequest);
                    s.writeTextMessage(JsonRpcWriter.writeErrorResponse(jsonRpcRequest.getId(), jsonRpcMethodName, e).encode());
                    return;
                }

                Cancellable cancellable = multi.subscribe()
                        .with(
                                item -> {
                                    JsonObject jsonResponse = JsonRpcWriter.writeResponse(jsonRpcRequest.getId(), item,
                                            MessageType.SubscriptionMessage);
                                    s.writeTextMessage(jsonResponse.encodePrettily());
                                },
                                failure -> {
                                    s.writeTextMessage(JsonRpcWriter
                                            .writeErrorResponse(jsonRpcRequest.getId(), jsonRpcMethodName, failure).encode());
                                    this.subscriptions.remove(jsonRpcRequest.getId());
                                },
                                () -> this.subscriptions.remove(jsonRpcRequest.getId()));

                this.subscriptions.put(jsonRpcRequest.getId(), cancellable);
                s.writeTextMessage(JsonRpcWriter.writeResponse(jsonRpcRequest.getId(), null, MessageType.Void).encode());
            } else {
                // The invocation will return a Uni<JsonObject>
                Uni<?> uni;
                try {
                    if (jsonRpcRequest.hasParams()) {
                        Object[] args = getArgsAsObjects(reflectionInfo.params, jsonRpcRequest);
                        uni = invoke(reflectionInfo, target, args);
                    } else {
                        uni = invoke(reflectionInfo, target, new Object[0]);
                    }
                } catch (Exception e) {
                    logger.errorf(e, "Unable to invoke method %s using JSON-RPC, request was: %s", jsonRpcMethodName,
                            jsonRpcRequest);
                    s.writeTextMessage(JsonRpcWriter.writeErrorResponse(jsonRpcRequest.getId(), jsonRpcMethodName, e).encode());
                    return;
                }
                uni.subscribe()
                        .with(item -> {
                            s.writeTextMessage(JsonRpcWriter.writeResponse(jsonRpcRequest.getId(), item,
                                    MessageType.Response).encode());
                        }, failure -> {
                            s.writeTextMessage(JsonRpcWriter
                                    .writeErrorResponse(jsonRpcRequest.getId(), jsonRpcMethodName, failure).encode());
                        });
            }
        } else {
            // Method not found
            s.writeTextMessage(JsonRpcWriter.writeMethodNotFoundResponse(jsonRpcRequest.getId(), jsonRpcMethodName).encode());
        }
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
