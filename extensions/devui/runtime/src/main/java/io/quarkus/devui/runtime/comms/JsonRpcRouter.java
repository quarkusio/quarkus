package io.quarkus.devui.runtime.comms;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.DefaultBean;
import io.quarkus.assistant.runtime.dev.Assistant;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devui.runtime.js.JavaScriptResponseWriter;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcCodec;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcMethod;
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

    private final Map<Integer, Cancellable> activeSubscriptions = new ConcurrentHashMap<>();

    // Map json-rpc method to java in runtime classpath
    private Map<String, JsonRpcMethod> runtimeMethodsMap;
    // Map json-rpc subscriptions to java in runtime classpath
    private Map<String, JsonRpcMethod> runtimeSubscriptionMap;

    // Map json-rpc method to java in deployment classpath
    private Map<String, JsonRpcMethod> deploymentMethodsMap;
    // Map json-rpc subscriptions to java in deployment classpath
    private Map<String, JsonRpcMethod> deploymentSubscriptionsMap;

    // Map json-rpc methods responses that is recorded
    private Map<String, JsonRpcMethod> recordedMethodsMap;
    // Map json-rpc subscriptions responses that is recorded
    private Map<String, JsonRpcMethod> recordedSubscriptionsMap;

    private static final List<JsonRpcResponseWriter> SESSIONS = Collections.synchronizedList(new ArrayList<>());
    private JsonRpcCodec codec;

    @Produces
    @DefaultBean
    public Optional<Assistant> defaultAssistant() {
        return Optional.empty();
    }

    /**
     * This gets populated at build time so the the routes knows all json-rpc endpoints.
     *
     * @param runtimeMethods
     * @param runtimeSubscriptions
     * @param deploymentMethods
     * @param deploymentSubscriptions
     * @param recordedMethods
     * @param recordedSubscriptions
     */
    public void populateJsonRpcEndpoints(Map<String, JsonRpcMethod> runtimeMethods,
            Map<String, JsonRpcMethod> runtimeSubscriptions,
            Map<String, JsonRpcMethod> deploymentMethods,
            Map<String, JsonRpcMethod> deploymentSubscriptions,
            Map<String, JsonRpcMethod> recordedMethods,
            Map<String, JsonRpcMethod> recordedSubscriptions) {

        this.runtimeMethodsMap = enhanceRuntimeJsonRpcEndpoints(runtimeMethods);
        this.runtimeSubscriptionMap = enhanceRuntimeJsonRpcEndpoints(runtimeSubscriptions);

        this.deploymentMethodsMap = deploymentMethods;
        this.deploymentSubscriptionsMap = deploymentSubscriptions;

        this.recordedMethodsMap = recordedMethods;
        this.recordedSubscriptionsMap = recordedSubscriptions;
    }

    public void initializeCodec(JsonMapper jsonMapper) {
        this.codec = new JsonRpcCodec(jsonMapper);
    }

    public void addSocket(ServerWebSocket socket) {
        JavaScriptResponseWriter writer = new JavaScriptResponseWriter(socket);
        SESSIONS.add(writer);
        socket.textMessageHandler((e) -> {
            JsonRpcRequest jsonRpcRequest = codec.readRequest(e);
            route(jsonRpcRequest, writer);
        }).closeHandler((e) -> {
            purge();
        });
        purge();
    }

    @Produces
    public JsonRpcCodec getJsonRpcCodec() {
        return this.codec;
    }

    public Map<String, JsonRpcMethod> getRuntimeMethodsMap() {
        return runtimeMethodsMap;
    }

    public Map<String, JsonRpcMethod> getRuntimeSubscriptionMap() {
        return runtimeSubscriptionMap;
    }

    public Map<String, JsonRpcMethod> getDeploymentMethodsMap() {
        return deploymentMethodsMap;
    }

    public Map<String, JsonRpcMethod> getDeploymentSubscriptionsMap() {
        return deploymentSubscriptionsMap;
    }

    public Map<String, JsonRpcMethod> getRecordedMethodsMap() {
        return recordedMethodsMap;
    }

    public Map<String, JsonRpcMethod> getRecordedSubscriptionsMap() {
        return recordedSubscriptionsMap;
    }

    void onStart(@Observes StartupEvent ev) {
        purge();
        for (JsonRpcResponseWriter jrrw : new ArrayList<>(SESSIONS)) {
            if (!jrrw.isClosed()) {
                codec.writeResponse(jrrw, -1, LocalDateTime.now().toString(), MessageType.HotReload);
            }
        }
    }

    private void purge() {
        SESSIONS.removeIf(JsonRpcResponseWriter::isClosed);
    }

    @Inject
    Logger logger;

    @SuppressWarnings("unchecked")
    public void route(JsonRpcRequest jsonRpcRequest, JsonRpcResponseWriter jrrw) {
        String jsonRpcMethodName = jsonRpcRequest.getMethod();

        if (jsonRpcMethodName.equalsIgnoreCase(UNSUBSCRIBE)) { // TODO: Move to protocol specific ?
            // This is a Dev UI subscription that terminated
            this.routeToDevUIUnsubscribe(jsonRpcRequest, jrrw);
        } else if (this.runtimeMethodsMap.containsKey(jsonRpcMethodName)) {
            // This is a Runtime method that needs to route to the extension
            this.routeToRuntimeMethod(jsonRpcRequest, jrrw);
        } else if (this.runtimeSubscriptionMap.containsKey(jsonRpcMethodName)) {
            // This is a Runtime subscription that needs to route to the extension
            this.routeToRuntimeSubscription(jsonRpcRequest, jrrw);
        } else if (this.deploymentMethodsMap.containsKey(jsonRpcMethodName)
                || this.deploymentSubscriptionsMap.containsKey(jsonRpcMethodName)
                || this.recordedMethodsMap.containsKey(jsonRpcMethodName)
                || this.recordedSubscriptionsMap.containsKey(jsonRpcMethodName)) {
            // This is Deployment method that needs to route to the extension
            this.routeToDeployment(jsonRpcRequest, jrrw);
        } else {
            // This is an error. Method not found
            codec.writeMethodNotFoundResponse(jrrw, jsonRpcRequest.getId(), jsonRpcMethodName);
        }
    }

    private void routeToDevUIUnsubscribe(JsonRpcRequest jsonRpcRequest, JsonRpcResponseWriter jrrw) {
        if (this.activeSubscriptions.containsKey(jsonRpcRequest.getId())) {
            Cancellable cancellable = this.activeSubscriptions.remove(jsonRpcRequest.getId());
            cancellable.cancel();
        }
        codec.writeResponse(jrrw, jsonRpcRequest.getId(), null, MessageType.Void);
    }

    private void routeToRuntimeMethod(JsonRpcRequest jsonRpcRequest, JsonRpcResponseWriter jrrw) {
        JsonRpcMethod runtimeJsonRpcMethod = this.runtimeMethodsMap.get(jsonRpcRequest.getMethod());
        Object target = Arc.container().select(runtimeJsonRpcMethod.getBean()).get(); // Lookup bean
        Uni<?> uni;
        try {
            Object[] args = new Object[0];
            if (jsonRpcRequest.hasParams()) {
                args = getArgsAsObjects(runtimeJsonRpcMethod.getParameters(), jsonRpcRequest);
            }
            uni = invoke(runtimeJsonRpcMethod, target, args);

        } catch (Exception e) {
            logger.errorf(e, "Unable to invoke method %s using JSON-RPC, request was: %s", jsonRpcRequest.getMethod(),
                    jsonRpcRequest);
            codec.writeErrorResponse(jrrw, jsonRpcRequest.getId(), jsonRpcRequest.getMethod(), e);
            return;
        }
        uni.subscribe()
                .with(item -> {
                    if (item != null && Map.class.isAssignableFrom(item.getClass())) {
                        Map map = (Map) item;
                        if (map.size() == 3 && map.containsKey("alreadySerialized") && map.containsKey("messageType")
                                && map.containsKey("response") && map.get("alreadySerialized").equals("true")) {
                            Object response = map.get("response");

                            // The message response was already serialized, write text directly to socket
                            jrrw.write("{\"id\":" + jsonRpcRequest.getId() + ",\"result\":{\"messageType\":\""
                                    + map.get("messageType") + "\",\"object\":" + response + "}}");
                        } else {
                            codec.writeResponse(jrrw, jsonRpcRequest.getId(), item, MessageType.Response);
                        }
                    } else {
                        codec.writeResponse(jrrw, jsonRpcRequest.getId(), item, MessageType.Response);
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
                    codec.writeErrorResponse(jrrw, jsonRpcRequest.getId(), jsonRpcRequest.getMethod(), actualFailure);
                });
    }

    private void routeToRuntimeSubscription(JsonRpcRequest jsonRpcRequest, JsonRpcResponseWriter jrrw) {
        JsonRpcMethod runtimeJsonRpcSubscription = this.runtimeSubscriptionMap.get(jsonRpcRequest.getMethod());
        Object target = Arc.container().select(runtimeJsonRpcSubscription.getBean()).get(); // Lookup bean

        if (this.activeSubscriptions.containsKey(jsonRpcRequest.getId())) {
            // Cancel and resubscribe
            Cancellable cancellable = this.activeSubscriptions.remove(jsonRpcRequest.getId());
            cancellable.cancel();
        }

        Multi<?> multi;
        try {
            if (jsonRpcRequest.hasParams()) {
                Object[] args = getArgsAsObjects(runtimeJsonRpcSubscription.getParameters(), jsonRpcRequest);
                multi = (Multi<?>) runtimeJsonRpcSubscription.getJavaMethod().invoke(target, args);
            } else {
                multi = (Multi<?>) runtimeJsonRpcSubscription.getJavaMethod().invoke(target);
            }
        } catch (Exception e) {
            logger.errorf(e, "Unable to invoke method %s using JSON-RPC, request was: %s", jsonRpcRequest.getMethod(),
                    jsonRpcRequest);
            codec.writeErrorResponse(jrrw, jsonRpcRequest.getId(), jsonRpcRequest.getMethod(), e);
            return;
        }

        Cancellable cancellable = multi.subscribe()
                .with(
                        item -> {
                            codec.writeResponse(jrrw, jsonRpcRequest.getId(), item, MessageType.SubscriptionMessage);
                        },
                        failure -> {
                            codec.writeErrorResponse(jrrw, jsonRpcRequest.getId(), jsonRpcRequest.getMethod(), failure);
                            this.activeSubscriptions.remove(jsonRpcRequest.getId());
                        },
                        () -> this.activeSubscriptions.remove(jsonRpcRequest.getId()));

        this.activeSubscriptions.put(jsonRpcRequest.getId(), cancellable);
        codec.writeResponse(jrrw, jsonRpcRequest.getId(), null, MessageType.Void);
    }

    private void routeToDeployment(JsonRpcRequest jsonRpcRequest, JsonRpcResponseWriter jrrw) {
        String jsonRpcMethodName = jsonRpcRequest.getMethod();

        if (this.activeSubscriptions.containsKey(jsonRpcRequest.getId())) {
            // Cancel and resubscribe
            Cancellable cancellable = this.activeSubscriptions.remove(jsonRpcRequest.getId());
            cancellable.cancel();
        }

        Object returnedObject = null;
        if (this.recordedMethodsMap.containsKey(jsonRpcMethodName)) {
            returnedObject = this.recordedMethodsMap.get(jsonRpcMethodName).getRuntimeValue().getValue();
        } else if (this.recordedSubscriptionsMap.containsKey(jsonRpcMethodName)) {
            returnedObject = this.recordedSubscriptionsMap.get(jsonRpcMethodName).getRuntimeValue().getValue();
        } else {
            returnedObject = DevConsoleManager.invoke(jsonRpcMethodName, getArgsAsMap(jsonRpcRequest));
        }
        if (returnedObject != null) {
            // Support for Mutiny is diffcult because we are between the runtime and deployment classpath.
            // Supporting something like CompletableFuture and Flow.Publisher that is in the JDK works fine
            if (returnedObject instanceof Flow.Publisher) {
                Flow.Publisher<?> publisher = (Flow.Publisher) returnedObject;

                Cancellable cancellable = Multi.createFrom().publisher(publisher).subscribe()
                        .with(
                                item -> {
                                    codec.writeResponse(jrrw, jsonRpcRequest.getId(), item, MessageType.SubscriptionMessage);
                                },
                                failure -> {
                                    codec.writeErrorResponse(jrrw, jsonRpcRequest.getId(), jsonRpcMethodName, failure);
                                    this.activeSubscriptions.remove(jsonRpcRequest.getId());
                                },
                                () -> this.activeSubscriptions.remove(jsonRpcRequest.getId()));

                this.activeSubscriptions.put(jsonRpcRequest.getId(), cancellable);
                codec.writeResponse(jrrw, jsonRpcRequest.getId(), null, MessageType.Void);
            } else if (returnedObject instanceof CompletionStage) {
                CompletionStage<?> future = (CompletionStage) returnedObject;
                future.thenAccept(r -> {
                    codec.writeResponse(jrrw, jsonRpcRequest.getId(), r,
                            MessageType.Response);
                }).exceptionally(throwable -> {
                    codec.writeErrorResponse(jrrw, jsonRpcRequest.getId(), jsonRpcMethodName, throwable);
                    return null;
                });
            } else {
                codec.writeResponse(jrrw, jsonRpcRequest.getId(), returnedObject,
                        MessageType.Response);
            }
        }
    }

    private Uni<?> invoke(JsonRpcMethod runtimeJsonRpcMethod, Object target, Object[] args) {
        Uni<?> uni;
        if (runtimeJsonRpcMethod.isReturningUni()) {
            try {
                uni = ((Uni<?>) runtimeJsonRpcMethod.getJavaMethod().invoke(target, args));
            } catch (Exception e) {
                return Uni.createFrom().failure(e);
            }
        } else if (runtimeJsonRpcMethod.isReturningCompletableFuture() || runtimeJsonRpcMethod.isReturningCompletionStage()) {
            try {
                uni = Uni.createFrom()
                        .completionStage((CompletionStage) runtimeJsonRpcMethod.getJavaMethod().invoke(target, args));
            } catch (Exception e) {
                return Uni.createFrom().failure(e);
            }
        } else {
            uni = Uni.createFrom()
                    .item(Unchecked.supplier(() -> runtimeJsonRpcMethod.getJavaMethod().invoke(target, args)));
        }
        if (!runtimeJsonRpcMethod.isIsExplicitlyNonBlocking()) {
            return uni.runSubscriptionOn(Infrastructure.getDefaultExecutor());
        } else {
            return uni;
        }
    }

    private Object[] getArgsAsObjects(Map<String, JsonRpcMethod.Parameter> parameters, JsonRpcRequest jsonRpcRequest) {
        List<Object> objects = new ArrayList<>();
        for (Map.Entry<String, JsonRpcMethod.Parameter> expectedParams : parameters.entrySet()) {
            String paramName = expectedParams.getKey();
            Class<?> paramType = expectedParams.getValue().getType();
            Object param = jsonRpcRequest.getParam(paramName, paramType);
            objects.add(param);
        }
        return objects.toArray(Object[]::new);
    }

    private Map<String, String> getArgsAsMap(JsonRpcRequest jsonRpcRequest) {
        if (jsonRpcRequest.hasParams()) {
            return (Map<String, String>) jsonRpcRequest.getParams();
        }
        return Map.of();
    }

    public JsonMapper getJsonMapper() {
        return codec.getJsonMapper();
    }

    private static final String UNSUBSCRIBE = "unsubscribe";

    /**
     * This goes though all runtime endpoints and get the correct Java method
     */
    private Map<String, JsonRpcMethod> enhanceRuntimeJsonRpcEndpoints(
            Map<String, JsonRpcMethod> runtimeMethods) {
        for (Map.Entry<String, JsonRpcMethod> method : runtimeMethods.entrySet()) {
            JsonRpcMethod jsonRpcMethod = method.getValue();

            @SuppressWarnings("unchecked")
            Object providerInstance = Arc.container().select(jsonRpcMethod.getBean()).get(); // This is just here so that we can get the methods

            try {
                Method javaMethod;
                if (jsonRpcMethod.hasParameters()) {
                    Class<?>[] types = jsonRpcMethod.getParameters().values().stream()
                            .map(p -> p.getType())
                            .toArray(Class<?>[]::new);
                    javaMethod = providerInstance.getClass().getMethod(jsonRpcMethod.getJavaMethodName(), types);
                } else {
                    javaMethod = providerInstance.getClass().getMethod(jsonRpcMethod.getJavaMethodName());
                }
                jsonRpcMethod.setJavaMethod(javaMethod);
            } catch (NoSuchMethodException | SecurityException ex) {
                throw new RuntimeException(ex);
            }
        }
        return runtimeMethods;
    }

}
