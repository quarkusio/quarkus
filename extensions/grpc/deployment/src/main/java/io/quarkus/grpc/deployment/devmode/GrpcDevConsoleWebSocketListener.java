package io.quarkus.grpc.deployment.devmode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.ServiceDescriptor;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.dev.testing.GrpcWebSocketProxy;
import io.vertx.core.json.JsonObject;

public class GrpcDevConsoleWebSocketListener implements GrpcWebSocketProxy.WebSocketListener {

    private static final Logger log = Logger.getLogger(GrpcDevConsoleWebSocketListener.class);

    private Map<String, Object> grpcClientStubs;
    private Map<String, ServiceDescriptor> serviceDescriptors;

    private final ClassLoader deploymentClassLoader;
    private final Collection<Class<?>> grpcServices;

    private final Map<Integer, WebSocketData> webSocketConnections = new ConcurrentHashMap<>();

    public GrpcDevConsoleWebSocketListener(Collection<Class<?>> grpcServices, ClassLoader deploymentClassLoader) {
        this.grpcServices = grpcServices;
        this.deploymentClassLoader = deploymentClassLoader;
    }

    private void handle(String input, WebSocketData websocketData) {
        ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(deploymentClassLoader);
        try {
            JsonObject grpcRequest = new JsonObject(input);

            // each message sent through this websocket has to have an ID
            // this is an ID of the gRPC method invocation
            // if client-side streaming is used, subsequent calls that should use the same
            // stream should have the same ID
            Integer id = grpcRequest.getInteger("id");
            String serviceName = grpcRequest.getString("serviceName");
            String methodName = grpcRequest.getString("methodName");

            if ("DISCONNECT".equals(grpcRequest.getString("command"))) {
                GrpcCallData grpcCall = websocketData.callsInProgress.get(id);
                if (grpcCall != null && grpcCall.incomingStream != null) {
                    grpcCall.incomingStream.onCompleted();
                }
                return;
            }

            GrpcCallData grpcCall;
            if (websocketData.callsInProgress.containsKey(id)) {
                grpcCall = websocketData.callsInProgress.get(id);
            } else {
                Optional<GrpcCallData> maybeOldCall = websocketData.callsInProgress.values()
                        .stream().filter(call -> call.methodName.equals(methodName) && call.serviceName.equals(serviceName))
                        .findAny();
                maybeOldCall.ifPresent(call -> {
                    if (call.incomingStream != null) {
                        call.incomingStream.onCompleted();
                    }
                    websocketData.callsInProgress.remove(call.requestId);
                });

                grpcCall = new GrpcCallData();
                grpcCall.serviceName = serviceName;
                grpcCall.methodName = methodName;
                grpcCall.requestId = id;
                websocketData.callsInProgress.put(grpcCall.requestId, grpcCall);
            }

            String testJsonData = grpcRequest.getString("content");
            Object grpcStub = grpcClientStubs.get(serviceName);

            if (grpcStub == null) {
                websocketData.responseConsumer.accept(jsonResponse(id, "NO_STUB").encode());
            } else {
                ServiceDescriptor serviceDescriptor = serviceDescriptors.get(serviceName);
                MethodDescriptor<?, ?> methodDescriptor = null;
                for (MethodDescriptor<?, ?> method : serviceDescriptor.getMethods()) {
                    if (method.getBareMethodName() != null && method.getBareMethodName().equals(methodName)) {
                        methodDescriptor = method;
                    }
                }
                if (methodDescriptor == null) {
                    websocketData.responseConsumer.accept(jsonResponse(id, "NO_DESCRIPTOR").encode());
                } else {
                    Method stubMethod = null;
                    String realMethodName = decapitalize(methodDescriptor.getBareMethodName());

                    for (Method method : grpcStub.getClass().getDeclaredMethods()) {
                        if (method.getName().equals(realMethodName)) {
                            stubMethod = method;
                        }
                    }

                    if (stubMethod == null) {
                        websocketData.responseConsumer.accept(jsonResponse(id, "NO_METHOD").encode());
                        log.error(realMethodName + " method not declared on the " + grpcStub.getClass());
                    } else {

                        // Identify the request class
                        MethodDescriptor.Marshaller<?> requestMarshaller = methodDescriptor.getRequestMarshaller();
                        if (requestMarshaller instanceof MethodDescriptor.PrototypeMarshaller) {
                            MethodDescriptor.PrototypeMarshaller<?> protoMarshaller = (MethodDescriptor.PrototypeMarshaller<?>) requestMarshaller;
                            Class<?> requestType = protoMarshaller.getMessagePrototype().getClass();

                            try {
                                // Create a new builder for the request message, e.g. HelloRequest.newBuilder()
                                Method newBuilderMethod = requestType.getDeclaredMethod("newBuilder");
                                Message.Builder builder = (Message.Builder) newBuilderMethod.invoke(null);

                                // Use the test data to build the request object
                                JsonFormat.parser().merge(testJsonData, builder);

                                Message message = builder.build();
                                if (grpcCall.incomingStream != null) {
                                    // we are already connected with this gRPC endpoint, just send the message
                                    grpcCall.incomingStream.onNext(message);
                                } else {
                                    // Invoke the stub method and format the response as JSON

                                    StreamObserver<?> responseObserver = new StreamObserver<Object>() {
                                        @Override
                                        public void onNext(Object value) {
                                            String body = null;
                                            try {
                                                body = JsonFormat.printer().print((MessageOrBuilder) value);
                                            } catch (InvalidProtocolBufferException e) {
                                                websocketData.responseConsumer
                                                        .accept(jsonResponse(id, "ERROR").put("body", e.getMessage()).encode());
                                                log.error("Failed to transform response to JSON", e);
                                            }
                                            JsonObject reply = jsonResponse(id, "PAYLOAD");
                                            reply.put("body", body);
                                            websocketData.responseConsumer.accept(reply.encode());
                                        }

                                        @Override
                                        public void onError(Throwable t) {
                                            websocketData.responseConsumer
                                                    .accept(jsonResponse(id, "ERROR").put("body", t.getMessage())
                                                            .encode());
                                            grpcCall.incomingStream = null;
                                            log.error("Failure returned by gRPC service", t);
                                        }

                                        @Override
                                        public void onCompleted() {
                                            websocketData.responseConsumer.accept(jsonResponse(id, "COMPLETED").encode());
                                            grpcCall.incomingStream = null;
                                        }
                                    };
                                    if (stubMethod.getParameterCount() == 1
                                            && stubMethod.getReturnType() == StreamObserver.class) {
                                        // returned StreamObserver consumes incoming messages
                                        //noinspection unchecked
                                        grpcCall.incomingStream = (StreamObserver<Message>) stubMethod.invoke(grpcStub,
                                                responseObserver);
                                        grpcCall.incomingStream.onNext(message);
                                    } else {
                                        // incoming message should be passed as the first parameter of the invocation
                                        stubMethod.invoke(grpcStub, message, responseObserver);
                                    }
                                }
                            } catch (Exception e) {
                                websocketData.responseConsumer
                                        .accept(jsonResponse(id, "ERROR").put("body",
                                                e.getMessage() + "\nCheck application log for more details")
                                                .encode());
                                grpcCall.incomingStream = null;
                                log.error("Failure returned by gRPC service", e);
                            }
                        }
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }

    static String decapitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) &&
                Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        char[] chars = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    private JsonObject jsonResponse(Integer id, String status) {
        return new JsonObject()
                .put("id", id)
                .put("status", status);
    }

    public void init() {
        Map<String, Object> serverConfig = DevConsoleManager.getGlobal("io.quarkus.grpc.serverConfig");

        if (serviceDescriptors != null) {
            return;
        }
        serviceDescriptors = new HashMap<>();
        grpcClientStubs = new HashMap<>();
        try {
            if (serverConfig == null || Boolean.FALSE.equals(serverConfig.get("ssl"))) {
                for (Class<?> grpcServiceClass : grpcServices) {

                    Method method = grpcServiceClass.getDeclaredMethod("getServiceDescriptor");
                    ServiceDescriptor serviceDescriptor = (ServiceDescriptor) method.invoke(null);
                    serviceDescriptors.put(serviceDescriptor.getName(), serviceDescriptor);

                    // TODO more config options
                    Channel channel = NettyChannelBuilder
                            .forAddress(serverConfig.get("host").toString(), (Integer) serverConfig.get("port"))
                            .usePlaintext()
                            .build();
                    Method stubFactoryMethod;

                    try {
                        stubFactoryMethod = grpcServiceClass.getDeclaredMethod("newStub", Channel.class);
                    } catch (NoSuchMethodException e) {
                        log.warnf("Ignoring gRPC service - newStub() method not declared on %s", grpcServiceClass);
                        continue;
                    }

                    Object stub = stubFactoryMethod.invoke(null, channel);
                    grpcClientStubs.put(serviceDescriptor.getName(), stub);
                }
            }
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to initialize client stubs for gRPC Dev UI");
        }
    }

    @Override
    public void onOpen(int id, Consumer<String> responseConsumer) {
        init();
        webSocketConnections.put(id, new WebSocketData(responseConsumer));
    }

    @Override
    public void newMessage(int id, String content) {
        WebSocketData webSocketData = webSocketConnections.get(id);
        if (webSocketData != null) {
            handle(content, webSocketData);
        } else {
            log.warn("gRPC Dev Console WebSocket message for an unregistered WebSocket id");
        }
    }

    @Override
    public void onClose(int id) {
        closeAllClients(id);

        webSocketConnections.remove(id);
    }

    private void closeAllClients(int id) {
        WebSocketData webSocketData = webSocketConnections.get(id);

        if (webSocketData != null) {
            for (GrpcCallData callData : webSocketData.callsInProgress.values()) {
                try {
                    callData.incomingStream.onCompleted();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static class GrpcCallData {
        Integer requestId;
        String serviceName;
        String methodName;
        StreamObserver<Message> incomingStream;
    }

    // contains information about all the connection done by a single
    // browser window, i.e. a single websocket
    private static class WebSocketData {
        final Consumer<String> responseConsumer;
        Map<Integer, GrpcCallData> callsInProgress = new HashMap<>();

        private WebSocketData(Consumer<String> responseConsumer) {
            this.responseConsumer = responseConsumer;
        }
    }
}
