package io.quarkus.grpc.transcoding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.inject.Instance;

import org.jboss.logging.Logger;

import com.google.protobuf.Message;

import io.grpc.BindableService;
import io.grpc.MethodDescriptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.quarkus.arc.Arc;
import io.quarkus.grpc.GrpcTranscoding;
import io.quarkus.grpc.GrpcTranscodingDescriptor;
import io.quarkus.grpc.auth.GrpcSecurityInterceptor;
import io.quarkus.grpc.runtime.GrpcContainer;
import io.quarkus.grpc.runtime.GrpcServerRecorder;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

@Recorder
public class GrpcTranscodingRecorder {

    private static final Logger LOGGER = Logger.getLogger(GrpcTranscodingRecorder.class.getName());

    public RuntimeValue<GrpcTranscodingServer> initializeMarshallingServer(RuntimeValue<Vertx> vertxSupplier,
            RuntimeValue<Router> routerSupplier,
            ShutdownContext shutdown, Map<String, List<GrpcTranscodingMethod>> httpMethods,
            boolean securityPresent) {
        GrpcTranscodingServer transcodingServer = new GrpcTranscodingServer(vertxSupplier.getValue());

        GrpcContainer grpcContainer = Arc.container().instance(GrpcContainer.class).get();
        GrpcTranscodingContainer container = Arc.container().instance(GrpcTranscodingContainer.class).get();

        if (grpcContainer == null) {
            throw new IllegalStateException("GrpcContainer not found");
        }

        if (container == null) {
            throw new IllegalStateException("GrpcTranscodingContainer not found");
        }

        List<GrpcServerRecorder.GrpcServiceDefinition> grpcServices = collectServiceDefinitions(grpcContainer.getServices());
        List<GrpcTranscoding> transcodingServices = collectTranscodingServices(container.getServices());

        List<ServerMethodDefinition<?, ?>> mappedMethods = new ArrayList<>();

        LOGGER.info("Initializing gRPC transcoding services");
        for (GrpcTranscoding transcodingService : transcodingServices) {
            GrpcServerRecorder.GrpcServiceDefinition grpcService = findGrpcService(grpcServices, transcodingService);
            List<GrpcTranscodingMethod> transcodingMethods = findTranscodingMethods(httpMethods,
                    transcodingService.getGrpcServiceName());

            for (ServerMethodDefinition<?, ?> serviceDefinition : grpcService.definition.getMethods()) {
                MethodDescriptor<Message, Message> methodDescriptor = (MethodDescriptor<Message, Message>) serviceDefinition
                        .getMethodDescriptor();
                GrpcTranscodingMethod transcodingMethod = findTranscodingMethod(transcodingMethods, methodDescriptor);

                String path = transcodingMethod.getUriTemplate();
                GrpcTranscodingMetadata<?, ?> metadata = createMetadata(transcodingMethod, methodDescriptor,
                        transcodingService);

                transcodingServer.addMethodMapping(path, methodDescriptor.getFullMethodName());
                transcodingServer.addMetadataHandler(methodDescriptor.getFullMethodName(), metadata);

                mappedMethods.add(serviceDefinition);

                Route route = routerSupplier.getValue().route().handler(ctx -> {
                    if (securityPresent) {
                        GrpcSecurityInterceptor.propagateSecurityIdentityWithDuplicatedCtx(ctx);
                    }
                    if (!Context.isOnEventLoopThread()) {
                        Context capturedVertxContext = Vertx.currentContext();
                        if (capturedVertxContext != null) {
                            capturedVertxContext.runOnContext(new Handler<Void>() {
                                @Override
                                public void handle(Void unused) {
                                    transcodingServer.handle(ctx.request());
                                }
                            });
                            return;
                        }
                    }

                    transcodingServer.handle(ctx.request());
                });

                shutdown.addShutdownTask(route::remove);
            }
        }

        GrpcTranscodingBridge bridge = new GrpcTranscodingBridge(mappedMethods);
        bridge.bind(transcodingServer);

        return new RuntimeValue<>(transcodingServer);
    }

    private <Req extends Message, Resp extends Message> GrpcTranscodingMetadata<Req, Resp> createMetadata(
            GrpcTranscodingMethod transcodingMethod, MethodDescriptor<Req, Resp> methodDescriptor,
            GrpcTranscoding transcodingService) {
        String fullMethodName = methodDescriptor.getFullMethodName()
                .substring(methodDescriptor.getFullMethodName().lastIndexOf("/") + 1);
        fullMethodName = Character.toLowerCase(fullMethodName.charAt(0)) + fullMethodName.substring(1);

        GrpcTranscodingDescriptor<Req, Resp> descriptor = transcodingService.findTranscodingDescriptor(fullMethodName);

        return new GrpcTranscodingMetadata<>(
                transcodingMethod.getHttpMethodName(),
                fullMethodName,
                descriptor.getRequestMarshaller(),
                descriptor.getResponseMarshaller(),
                methodDescriptor);
    }

    private List<GrpcTranscodingMethod> findTranscodingMethods(Map<String, List<GrpcTranscodingMethod>> transcodingMethods,
            String grpcServiceName) {
        List<GrpcTranscodingMethod> methods = new ArrayList<>();
        for (Map.Entry<String, List<GrpcTranscodingMethod>> entry : transcodingMethods.entrySet()) {
            if (entry.getKey().startsWith(grpcServiceName)) {
                methods.addAll(entry.getValue());
            }
        }

        return methods;
    }

    private GrpcTranscodingMethod findTranscodingMethod(List<GrpcTranscodingMethod> transcodingMethods,
            MethodDescriptor<?, ?> methodDescriptor) {
        String fullMethodName = methodDescriptor.getFullMethodName();
        fullMethodName = fullMethodName.substring(fullMethodName.lastIndexOf("/") + 1);
        fullMethodName = Character.toLowerCase(fullMethodName.charAt(0)) + fullMethodName.substring(1);

        for (GrpcTranscodingMethod transcodingMethod : transcodingMethods) {
            if (transcodingMethod.getGrpcMethodName().startsWith(fullMethodName)) {
                return transcodingMethod;
            }
        }

        throw new IllegalStateException("Transcoding method not found for " + fullMethodName);
    }

    private static List<GrpcServerRecorder.GrpcServiceDefinition> collectServiceDefinitions(
            Instance<BindableService> services) {
        List<GrpcServerRecorder.GrpcServiceDefinition> definitions = new ArrayList<>();
        for (BindableService service : services) {
            ServerServiceDefinition definition = service.bindService();
            definitions.add(new GrpcServerRecorder.GrpcServiceDefinition(service, definition));
        }

        return definitions;
    }

    private static List<GrpcTranscoding> collectTranscodingServices(Instance<GrpcTranscoding> services) {
        List<GrpcTranscoding> transcodingServices = new ArrayList<>();
        for (GrpcTranscoding service : services) {
            transcodingServices.add(service);
        }

        return transcodingServices;
    }

    private static GrpcServerRecorder.GrpcServiceDefinition findGrpcService(
            List<GrpcServerRecorder.GrpcServiceDefinition> grpcServices, GrpcTranscoding transcodingService) {
        for (GrpcServerRecorder.GrpcServiceDefinition grpcService : grpcServices) {
            if (grpcService.getImplementationClassName().startsWith(transcodingService.getGrpcServiceName())) {
                return grpcService;
            }
        }

        throw new IllegalStateException("gRPC service not found for " + transcodingService.getGrpcServiceName());
    }
}
