package io.quarkus.grpc.deployment.devmode;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.MethodDescriptor.Marshaller;
import io.grpc.MethodDescriptor.PrototypeMarshaller;
import io.grpc.ServiceDescriptor;
import io.grpc.netty.NettyChannelBuilder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.runtime.BeanLookupSupplier;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.grpc.deployment.GrpcDotNames;
import io.quarkus.grpc.protoc.plugin.MutinyGrpcGenerator;
import io.quarkus.grpc.runtime.devmode.GrpcDevConsoleRecorder;
import io.quarkus.grpc.runtime.devmode.GrpcServices;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class GrpcDevConsoleProcessor {

    private static final Logger LOG = Logger.getLogger(GrpcDevConsoleProcessor.class);

    @BuildStep(onlyIf = IsDevelopment.class)
    public void devConsoleInfo(BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<DevConsoleRuntimeTemplateInfoBuildItem> infos) {
        beans.produce(AdditionalBeanBuildItem.unremovableOf(GrpcServices.class));
        infos.produce(
                new DevConsoleRuntimeTemplateInfoBuildItem("grpcServices",
                        new BeanLookupSupplier(GrpcServices.class)));
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public void collectMessagePrototypes(CombinedIndexBuildItem index,
            // Dummy producer to ensure the build step is executed
            BuildProducer<ServiceStartBuildItem> service)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, InvalidProtocolBufferException {
        Map<String, String> messagePrototypes = new HashMap<>();

        for (Class<?> grpcServiceClass : getGrpcServices(index.getIndex())) {

            Method method = grpcServiceClass.getDeclaredMethod("getServiceDescriptor");
            ServiceDescriptor serviceDescriptor = (ServiceDescriptor) method.invoke(null);

            for (MethodDescriptor<?, ?> methodDescriptor : serviceDescriptor.getMethods()) {
                Marshaller<?> requestMarshaller = methodDescriptor.getRequestMarshaller();
                if (requestMarshaller instanceof PrototypeMarshaller) {
                    PrototypeMarshaller<?> protoMarshaller = (PrototypeMarshaller<?>) requestMarshaller;
                    Object prototype = protoMarshaller.getMessagePrototype();
                    messagePrototypes.put(methodDescriptor.getFullMethodName() + "_REQUEST",
                            JsonFormat.printer().includingDefaultValueFields().print((MessageOrBuilder) prototype));
                }
            }
        }
        DevConsoleManager.setGlobal("io.quarkus.grpc.messagePrototypes", messagePrototypes);

    }

    @Record(value = RUNTIME_INIT)
    @BuildStep
    DevConsoleRouteBuildItem registerTestEndpoint(GrpcDevConsoleRecorder recorder, CombinedIndexBuildItem index)
            throws ClassNotFoundException, NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        // Store the server config so that it can be used in the test endpoint handler
        recorder.setServerConfiguration();
        return new DevConsoleRouteBuildItem("test", "POST", new TestEndpointHandler(getGrpcServices(index.getIndex())), true);
    }

    static class TestEndpointHandler implements Handler<RoutingContext> {

        private Map<String, Object> blockingStubs;
        private Map<String, ServiceDescriptor> serviceDescriptors;
        private final Collection<Class<?>> grpcServiceClasses;

        TestEndpointHandler(Collection<Class<?>> grpcServiceClasses) {
            this.grpcServiceClasses = grpcServiceClasses;
        }

        void init() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,
                InvocationTargetException {
            if (blockingStubs == null) {
                blockingStubs = new HashMap<>();
                serviceDescriptors = new HashMap<>();

                Map<String, Object> serverConfig = DevConsoleManager.getGlobal("io.quarkus.grpc.serverConfig");

                if (Boolean.FALSE.equals(serverConfig.get("ssl"))) {
                    for (Class<?> grpcServiceClass : grpcServiceClasses) {

                        Method method = grpcServiceClass.getDeclaredMethod("getServiceDescriptor");
                        ServiceDescriptor serviceDescriptor = (ServiceDescriptor) method.invoke(null);
                        serviceDescriptors.put(serviceDescriptor.getName(), serviceDescriptor);

                        // TODO more config options
                        Channel channel = NettyChannelBuilder
                                .forAddress(serverConfig.get("host").toString(), (Integer) serverConfig.get("port"))
                                .usePlaintext()
                                .build();
                        Method blockingStubFactoryMethod;

                        try {
                            blockingStubFactoryMethod = grpcServiceClass.getDeclaredMethod("newBlockingStub", Channel.class);
                        } catch (NoSuchMethodException e) {
                            LOG.warnf("Ignoring gRPC service - newBlockingStub() method not declared on %s", grpcServiceClass);
                            continue;
                        }

                        Object blockingStub = blockingStubFactoryMethod.invoke(null, channel);
                        blockingStubs.put(serviceDescriptor.getName(), blockingStub);
                    }
                }
            }
        }

        @Override
        public void handle(RoutingContext context) {
            try {
                // Lazily initialize the handler
                init();
            } catch (Exception e) {
                throw new IllegalStateException("Unable to initialize the test endpoint handler");
            }

            String serviceName = context.request().getParam("serviceName");
            String methodName = context.request().getParam("methodName");
            String testJsonData = context.getBodyAsString();

            Object blockingStub = blockingStubs.get(serviceName);

            if (blockingStub == null) {
                error(context, "No blocking stub found for: " + serviceName);
            } else {
                ServiceDescriptor serviceDescriptor = serviceDescriptors.get(serviceName);
                MethodDescriptor<?, ?> methodDescriptor = null;
                for (MethodDescriptor<?, ?> method : serviceDescriptor.getMethods()) {
                    if (method.getBareMethodName().equals(methodName)) {
                        methodDescriptor = method;
                    }
                }

                if (methodDescriptor == null) {
                    error(context, "No method descriptor found for: " + serviceName + "/" + methodName);
                } else {

                    // We need to find the correct method declared on the blocking stub
                    Method stubMethod = null;
                    String realMethodName = decapitalize(methodDescriptor.getBareMethodName());

                    for (Method method : blockingStub.getClass().getDeclaredMethods()) {
                        if (method.getName().equals(realMethodName)) {
                            stubMethod = method;
                        }
                    }

                    if (stubMethod == null) {
                        error(context, realMethodName + " method not declared on the " + blockingStub.getClass());
                    } else {

                        // Identify the request class
                        Marshaller<?> requestMarshaller = methodDescriptor.getRequestMarshaller();
                        if (requestMarshaller instanceof PrototypeMarshaller) {
                            PrototypeMarshaller<?> protoMarshaller = (PrototypeMarshaller<?>) requestMarshaller;
                            Class<?> requestType = protoMarshaller.getMessagePrototype().getClass();

                            try {
                                // Create a new builder for the request message, e.g. HelloRequest.newBuilder()
                                Method newBuilderMethod = requestType.getDeclaredMethod("newBuilder");
                                Message.Builder builder = (Builder) newBuilderMethod.invoke(null);
                                ;

                                // Use the test data to build the request object
                                JsonFormat.parser().merge(testJsonData, builder);

                                // Invoke the blocking stub method and format the response as JSON
                                Object response = stubMethod.invoke(blockingStub, builder.build());
                                context.response().putHeader("Content-Type", "application/json");
                                context.end(JsonFormat.printer().print((MessageOrBuilder) response));

                            } catch (Exception e) {
                                throw new IllegalStateException(e);
                            }
                        } else {
                            error(context, "Unable to identify the request type for: " + methodDescriptor);
                        }
                    }
                }
            }

        }
    }

    static void error(RoutingContext rc, String message) {
        LOG.warn(message);
        rc.response().setStatusCode(500).end(message);
    }

    Collection<Class<?>> getGrpcServices(IndexView index) throws ClassNotFoundException {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Set<String> serviceClassNames = new HashSet<>();
        for (ClassInfo mutinyGrpc : index.getAllKnownImplementors(GrpcDotNames.MUTINY_GRPC)) {
            // Find the original impl class
            // e.g. examples.MutinyGreeterGrpc -> examples.GreeterGrpc
            DotName originalImplName = DotName
                    .createSimple(mutinyGrpc.name().toString().replace(MutinyGrpcGenerator.CLASS_PREFIX, ""));
            ClassInfo originalImpl = index.getClassByName(originalImplName);
            if (originalImpl == null) {
                throw new IllegalStateException(
                        "The original implementation class of a gRPC service not found:" + originalImplName);
            }
            // Must declare static io.grpc.ServiceDescriptor getServiceDescriptor()
            MethodInfo getServiceDescriptor = originalImpl.method("getServiceDescriptor");
            if (getServiceDescriptor != null && Modifier.isStatic(getServiceDescriptor.flags())
                    && getServiceDescriptor.returnType().name().toString().equals(ServiceDescriptor.class.getName())) {
                serviceClassNames.add(getServiceDescriptor.declaringClass().name().toString());
            }
        }
        List<Class<?>> serviceClasses = new ArrayList<>();
        for (String className : serviceClassNames) {
            serviceClasses.add(tccl.loadClass(className));
        }
        return serviceClasses;
    }

    static String decapitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) &&
                Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        char chars[] = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

}
