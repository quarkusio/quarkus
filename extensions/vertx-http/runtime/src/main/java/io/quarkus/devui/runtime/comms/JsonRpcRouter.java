package io.quarkus.devui.runtime.comms;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcCodec;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcMethod;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcMethodName;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcRequest;
import io.quarkus.devui.runtime.jsonrpc.json.JsonMapper;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.core.http.ServerWebSocket;

/**
 * Route JsonRPC message to the correct method
 */
public class JsonRpcRouter {

    private final Map<Integer, Cancellable> subscriptions = new ConcurrentHashMap<>();

    // Map json-rpc method to java
    private final Map<String, ReflectionInfo> jsonRpcToJava = new HashMap<>();

    private static final List<ServerWebSocket> SESSIONS = Collections.synchronizedList(new ArrayList<>());
    private JsonRpcCodec codec;

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

    public void initializeCodec(JsonMapper jsonMapper) {
        this.codec = new JsonRpcCodec(jsonMapper);
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
        SESSIONS.add(socket);
        socket.textMessageHandler((e) -> {
            JsonRpcRequest jsonRpcRequest = codec.readRequest(e);
            route(jsonRpcRequest, socket);
        }).closeHandler((e) -> {
            purge();
        });
        purge();
    }

    void onStart(@Observes StartupEvent ev) {
        purge();
        for (ServerWebSocket s : new ArrayList<>(SESSIONS)) {
            if (!s.isClosed()) {
                codec.writeResponse(s, -1, LocalDateTime.now().toString(), MessageType.HotReload);
            }
        }
    }

    private void purge() {
        for (ServerWebSocket s : new ArrayList<>(SESSIONS)) {
            if (s.isClosed()) {
                SESSIONS.remove(s);
            }
        }
    }

    @Inject
    Logger logger;

    @SuppressWarnings("unchecked")
    private void route(JsonRpcRequest jsonRpcRequest, ServerWebSocket s) {
        String jsonRpcMethodName = jsonRpcRequest.getMethod();

        // First check some internal methods
        if (jsonRpcMethodName.equalsIgnoreCase(UNSUBSCRIBE)) {
            if (this.subscriptions.containsKey(jsonRpcRequest.getId())) {
                Cancellable cancellable = this.subscriptions.remove(jsonRpcRequest.getId());
                cancellable.cancel();
            }
            codec.writeResponse(s, jsonRpcRequest.getId(), null, MessageType.Void);
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
                    codec.writeErrorResponse(s, jsonRpcRequest.getId(), jsonRpcMethodName, e);
                    return;
                }

                Cancellable cancellable = multi.subscribe()
                        .with(
                                item -> {
                                    codec.writeResponse(s, jsonRpcRequest.getId(), item, MessageType.SubscriptionMessage);
                                },
                                failure -> {
                                    codec.writeErrorResponse(s, jsonRpcRequest.getId(), jsonRpcMethodName, failure);
                                    this.subscriptions.remove(jsonRpcRequest.getId());
                                },
                                () -> this.subscriptions.remove(jsonRpcRequest.getId()));

                this.subscriptions.put(jsonRpcRequest.getId(), cancellable);
                codec.writeResponse(s, jsonRpcRequest.getId(), null, MessageType.Void);
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
                    codec.writeErrorResponse(s, jsonRpcRequest.getId(), jsonRpcMethodName, e);
                    return;
                }
                uni.subscribe()
                        .with(item -> {
                            if (item != null && JsonRpcMessage.class.isAssignableFrom(item.getClass())) {
                                JsonRpcMessage jsonRpcMessage = (JsonRpcMessage) item;
                                codec.writeResponse(s, jsonRpcRequest.getId(), jsonRpcMessage.getResponse(),
                                        jsonRpcMessage.getMessageType());
                            } else {
                                codec.writeResponse(s, jsonRpcRequest.getId(), item,
                                        MessageType.Response);
                            }
                        }, failure -> {
                            Throwable actualFailure;
                            // If the jsonrpc method is actually
                            // synchronous, the failure is wrapped in an
                            // InvocationTargetException, so unwrap it here
                            if (failure instanceof InvocationTargetException f) {
                                actualFailure = f.getTargetException();
                            } else if (failure.getCause() != null
                                    && failure.getCause() instanceof InvocationTargetException f) {
                                actualFailure = f.getTargetException();
                            } else {
                                actualFailure = failure;
                            }
                            codec.writeErrorResponse(s, jsonRpcRequest.getId(), jsonRpcMethodName, actualFailure);
                        });
            }
        } else {
            // Method not found
            codec.writeMethodNotFoundResponse(s, jsonRpcRequest.getId(), jsonRpcMethodName);
        }
    }

    private Object[] getArgsAsObjects(Map<String, Class> params, JsonRpcRequest jsonRpcRequest) {
        List<Object> objects = new ArrayList<>();
        for (Map.Entry<String, Class> expectedParams : params.entrySet()) {
            String paramName = expectedParams.getKey();
            Class paramType = expectedParams.getValue();
            Object param = jsonRpcRequest.getParam(paramName, paramType);
            objects.add(param);
        }
        return objects.toArray(Object[]::new);
    }

    private static final String DOT = ".";
    private static final String UNSUBSCRIBE = "unsubscribe";

}
