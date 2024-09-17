package io.quarkus.grpc.runtime.devui;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.ServiceDescriptor;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.grpc.runtime.config.GrpcConfiguration;
import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.quarkus.grpc.runtime.devmode.GrpcServices;
import io.quarkus.vertx.http.runtime.CertificateConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * We should consider to use gRPC directly from the Javascript client.
 * At the moment we send the data over json-rpc (web socket) to just create a Java gRPC client that calls the gRPC server
 * method.
 * We can just call the server method directly from Javascript.
 * See @grpc/grpc-js
 */
public class GrpcJsonRPCService {
    private static final Logger LOG = Logger.getLogger(GrpcJsonRPCService.class);

    private Map<String, GrpcServiceClassInfo> grpcServiceClassInfos;
    private Map<String, StreamObserver<Message>> callsInProgress;

    @Inject
    HttpConfiguration httpConfiguration;

    @Inject
    GrpcConfiguration grpcConfiguration;

    @Inject
    GrpcServices grpcServices;

    private String host;
    private int port;
    private boolean ssl;

    @PostConstruct
    public void init() {
        GrpcServerConfiguration serverConfig = grpcConfiguration.server;
        if (serverConfig.useSeparateServer) {
            this.host = serverConfig.host;
            this.port = serverConfig.port;
            this.ssl = serverConfig.ssl.certificate.isPresent() || serverConfig.ssl.keyStore.isPresent();
        } else {
            this.host = httpConfiguration.host;
            this.port = httpConfiguration.port;
            this.ssl = isTLSConfigured(httpConfiguration.ssl.certificate);
        }
        this.grpcServiceClassInfos = getGrpcServiceClassInfos();
        this.callsInProgress = new HashMap<>();
    }

    private boolean isTLSConfigured(CertificateConfig certificate) {
        return certificate.files.isPresent()
                || certificate.keyFiles.isPresent()
                || certificate.keyStoreFile.isPresent();
    }

    public JsonArray getServices() {
        JsonArray services = new JsonArray();
        List<GrpcServices.ServiceDefinitionAndStatus> infos = this.grpcServices.getInfos();

        for (GrpcServices.ServiceDefinitionAndStatus info : infos) {
            JsonObject service = new JsonObject();
            service.put("status", info.status);
            service.put("name", info.getName());
            service.put("serviceClass", info.getServiceClass());
            service.put("hasTestableMethod", info.hasTestableMethod());

            JsonArray methods = new JsonArray();
            for (GrpcServices.MethodAndPrototype methodAndPrototype : info.getMethodsWithPrototypes()) {
                JsonObject method = new JsonObject();
                method.put("bareMethodName", methodAndPrototype.getBareMethodName());
                method.put("type", methodAndPrototype.getType());
                method.put("prototype", methodAndPrototype.getPrototype());
                method.put("isTestable", methodAndPrototype.isTestable());
                methods.add(method);
            }
            service.put("methods", methods);
            services.add(service);
        }

        return services;
    }

    public Uni<String> testService(String id, String serviceName, String methodName, String content) {
        try {
            return streamService(id, serviceName, methodName, false, content).toUni();
        } catch (Throwable t) {
            return Uni.createFrom().item(error(t.getMessage()).encodePrettily());
        }
    }

    public Multi<String> streamService(String id, String serviceName, String methodName, boolean isRunning,
            String content)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InvalidProtocolBufferException {
        if (content == null) {
            return Multi.createFrom().item(error("Invalid message").encodePrettily());
        }

        BroadcastProcessor<String> streamEvent = BroadcastProcessor.create();

        GrpcServiceClassInfo info = this.grpcServiceClassInfos.get(serviceName);

        ManagedChannel channel = getChannel(host, port);
        Object grpcStub = createStub(info.grpcServiceClass, channel);

        ServiceDescriptor serviceDescriptor = info.serviceDescriptor;

        final MethodDescriptor<?, ?> methodDescriptor = getMethodDescriptor(serviceDescriptor, methodName);
        MethodDescriptor.Marshaller<?> requestMarshaller = methodDescriptor.getRequestMarshaller();
        MethodDescriptor.PrototypeMarshaller<?> protoMarshaller = (MethodDescriptor.PrototypeMarshaller<?>) requestMarshaller;
        Class<?> requestType = protoMarshaller.getMessagePrototype().getClass();

        Message message = createMessage(content, requestType);

        if (isRunning) {
            // we are already connected with this gRPC endpoint, just send the message
            callsInProgress.get(id).onNext(message);
        } else {
            // Invoke the stub method and format the response as JSON
            StreamObserver<?> responseObserver = new TestObserver<>(streamEvent);
            StreamObserver<Message> incomingStream;

            final Method stubMethod = getStubMethod(grpcStub, methodDescriptor.getBareMethodName());

            if (stubMethod.getParameterCount() == 1 && stubMethod.getReturnType() == StreamObserver.class) {
                // returned StreamObserver consumes incoming messages
                //noinspection unchecked
                incomingStream = (StreamObserver<Message>) stubMethod.invoke(grpcStub, responseObserver);
                callsInProgress.put(id, incomingStream);
                // will be streamed continuously
                incomingStream.onNext(message);
            } else {
                // incoming message should be passed as the first parameter of the invocation
                stubMethod.invoke(grpcStub, message, responseObserver);
            }
        }

        channel.shutdown();
        return streamEvent;
    }

    private static Message createMessage(String content, Class<?> requestType)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InvalidProtocolBufferException {
        // Create a new builder for the request message, e.g. HelloRequest.newBuilder()
        Method newBuilderMethod = requestType.getDeclaredMethod("newBuilder");
        Message.Builder builder = (Message.Builder) newBuilderMethod.invoke(null);

        // Use the test data to build the request object
        JsonFormat.parser().merge(content, builder);
        return builder.build();
    }

    public Uni<Void> disconnectService(String id) {
        callsInProgress.get(id).onCompleted();
        callsInProgress.remove(id);
        return Uni.createFrom().voidItem();
    }

    private Map<String, GrpcJsonRPCService.GrpcServiceClassInfo> getGrpcServiceClassInfos() {
        Set<String> serviceClassNames = DevConsoleManager.getGlobal("io.quarkus.grpc.serviceClassNames");
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Map<String, GrpcJsonRPCService.GrpcServiceClassInfo> m = new HashMap<>();
        for (String className : serviceClassNames) {
            try {
                Class<?> grpcServiceClass = tccl.loadClass(className);
                ServiceDescriptor serviceDescriptor = createServiceDescriptor(grpcServiceClass);
                GrpcJsonRPCService.GrpcServiceClassInfo s = new GrpcJsonRPCService.GrpcServiceClassInfo(serviceDescriptor,
                        grpcServiceClass);
                m.put(serviceDescriptor.getName(), s);
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        }
        return m;
    }

    private ServiceDescriptor createServiceDescriptor(Class<?> grpcServiceClass) {
        try {
            Method method = grpcServiceClass.getDeclaredMethod("getServiceDescriptor");
            return (ServiceDescriptor) method.invoke(null);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            LOG.warnf("Could not create stub for %s - " + e.getMessage(), grpcServiceClass);
            return null;
        }
    }

    private JsonObject error(String message) {
        LOG.error(message);
        JsonObject error = new JsonObject();
        error.put("status", "ERROR");
        error.put("message", message);
        return error;
    }

    private Method getStubMethod(Object grpcStub, String bareMethodName) {
        String realMethodName = decapitalize(bareMethodName);

        for (Method method : grpcStub.getClass().getDeclaredMethods()) {
            if (method.getName().equals(realMethodName)) {
                return method;
            }
        }
        return null;
    }

    private String decapitalize(String name) {
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

    private MethodDescriptor getMethodDescriptor(ServiceDescriptor serviceDescriptor, String methodName) {
        for (MethodDescriptor<?, ?> method : serviceDescriptor.getMethods()) {
            if (method.getBareMethodName() != null && method.getBareMethodName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    private Object createStub(Class<?> grpcServiceClass, Channel channel) {
        try {
            Method stubFactoryMethod = grpcServiceClass.getDeclaredMethod("newStub", Channel.class);
            return stubFactoryMethod.invoke(null, channel);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            LOG.warnf("Could not create stub for %s - " + e.getMessage(), grpcServiceClass);
            return null;
        }
    }

    private ManagedChannel getChannel(String host, int port) {
        return NettyChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
    }

    private class TestObserver<Object> implements StreamObserver<Object> {
        private BroadcastProcessor<String> broadcaster;

        public TestObserver(BroadcastProcessor<String> broadcaster) {
            this.broadcaster = broadcaster;
        }

        @Override
        public void onNext(Object value) {
            try {
                String body = JsonFormat.printer().omittingInsignificantWhitespace().print((MessageOrBuilder) value);
                this.broadcaster.onNext(body);
            } catch (InvalidProtocolBufferException e) {
                this.broadcaster.onError(e);
            }
        }

        @Override
        public void onError(Throwable t) {
            this.broadcaster.onError(t);
        }

        @Override
        public void onCompleted() {
            this.broadcaster.onComplete();
        }
    }

    public static final class GrpcServiceClassInfo {
        public ServiceDescriptor serviceDescriptor;
        public Class<?> grpcServiceClass;

        public GrpcServiceClassInfo(ServiceDescriptor serviceDescriptor, Class<?> grpcServiceClass) {
            this.serviceDescriptor = serviceDescriptor;
            this.grpcServiceClass = grpcServiceClass;
        }
    }
}
