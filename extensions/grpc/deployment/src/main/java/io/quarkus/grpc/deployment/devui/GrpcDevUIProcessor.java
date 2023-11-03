package io.quarkus.grpc.deployment.devui;

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
import java.util.concurrent.Flow;

import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

import grpc.health.v1.HealthGrpc;
import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.ServiceDescriptor;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.grpc.deployment.DelegatingGrpcBeanBuildItem;
import io.quarkus.grpc.deployment.GrpcDotNames;
import io.quarkus.grpc.protoc.plugin.MutinyGrpcGenerator;
import io.quarkus.grpc.runtime.devmode.CollectStreams;
import io.quarkus.grpc.runtime.devmode.DelegatingGrpcBeansStorage;
import io.quarkus.grpc.runtime.devmode.GrpcServices;
import io.quarkus.grpc.runtime.devmode.StreamCollectorInterceptor;
import io.quarkus.grpc.runtime.devui.GrpcJsonRPCService;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

public class GrpcDevUIProcessor {
    private static final Logger log = Logger.getLogger(GrpcDevUIProcessor.class);

    @BuildStep(onlyIf = IsDevelopment.class)
    public AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(GrpcServices.class)
                .addBeanClasses(StreamCollectorInterceptor.class, CollectStreams.class)
                .build();
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void prepareDelegatingBeanStorage(
            List<DelegatingGrpcBeanBuildItem> delegatingBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans) {
        String className = "io.quarkus.grpc.internal.DelegatingGrpcBeansStorageImpl";
        try (ClassCreator classCreator = ClassCreator.builder()
                .className(className)
                .classOutput(new GeneratedBeanGizmoAdaptor(generatedBeans))
                .superClass(DelegatingGrpcBeansStorage.class)
                .build()) {
            classCreator.addAnnotation(Singleton.class.getName());
            MethodCreator constructor = classCreator
                    .getMethodCreator(io.quarkus.gizmo.MethodDescriptor.ofConstructor(className));
            constructor.invokeSpecialMethod(io.quarkus.gizmo.MethodDescriptor.ofConstructor(DelegatingGrpcBeansStorage.class),
                    constructor.getThis());

            for (DelegatingGrpcBeanBuildItem delegatingBean : delegatingBeans) {
                constructor.invokeVirtualMethod(
                        io.quarkus.gizmo.MethodDescriptor.ofMethod(DelegatingGrpcBeansStorage.class, "addDelegatingMapping",
                                void.class,
                                String.class, String.class),
                        constructor.getThis(),
                        constructor.load(delegatingBean.userDefinedBean.name().toString()),
                        constructor.load(delegatingBean.generatedBean.name().toString()));
            }
            constructor.returnValue(null);
        }

        unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(className));
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public void collectMessagePrototypes(CombinedIndexBuildItem index,
            // Dummy producer to ensure the build step is executed
            BuildProducer<ServiceStartBuildItem> service)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, InvalidProtocolBufferException {
        Map<String, String> messagePrototypes = new HashMap<>();

        Collection<Class<?>> grpcServices = getGrpcServices(index.getIndex());
        for (Class<?> grpcServiceClass : grpcServices) {

            Method method = grpcServiceClass.getDeclaredMethod("getServiceDescriptor");
            ServiceDescriptor serviceDescriptor = (ServiceDescriptor) method.invoke(null);

            for (MethodDescriptor<?, ?> methodDescriptor : serviceDescriptor.getMethods()) {
                MethodDescriptor.Marshaller<?> requestMarshaller = methodDescriptor.getRequestMarshaller();
                if (requestMarshaller instanceof MethodDescriptor.PrototypeMarshaller) {
                    MethodDescriptor.PrototypeMarshaller<?> protoMarshaller = (MethodDescriptor.PrototypeMarshaller<?>) requestMarshaller;
                    Object prototype = protoMarshaller.getMessagePrototype();
                    messagePrototypes.put(methodDescriptor.getFullMethodName() + "_REQUEST",
                            JsonFormat.printer().includingDefaultValueFields().print((MessageOrBuilder) prototype));
                }
            }
        }
        DevConsoleManager.setGlobal("io.quarkus.grpc.messagePrototypes", messagePrototypes);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    AnnotationsTransformerBuildItem transformUserDefinedServices(CombinedIndexBuildItem combinedIndexBuildItem) {
        Set<DotName> servicesToTransform = new HashSet<>();
        IndexView index = combinedIndexBuildItem.getIndex();
        for (AnnotationInstance annotation : index.getAnnotations(GrpcDotNames.GRPC_SERVICE)) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                ClassInfo serviceClass = annotation.target().asClass();
                // Transform a service if it's using the grpc-java API directly:
                // 1. Must not implement MutinyService
                if (getRawTypesInHierarchy(serviceClass, index).contains(GrpcDotNames.MUTINY_SERVICE)) {
                    continue;
                }
                // 2. The enclosing class of an extended class that implements BindableService must not implement MutinyGrpc
                ClassInfo abstractBindableService = findAbstractBindableService(serviceClass, index);
                if (abstractBindableService != null) {
                    ClassInfo enclosingClass = serviceClass.enclosingClass() != null
                            ? index.getClassByName(serviceClass.enclosingClass())
                            : null;
                    if (enclosingClass != null
                            && getRawTypesInHierarchy(enclosingClass, index).contains(GrpcDotNames.MUTINY_GRPC)) {
                        continue;
                    }
                }
                servicesToTransform.add(annotation.target().asClass().name());
            }
        }
        if (servicesToTransform.isEmpty()) {
            return null;
        }
        return new AnnotationsTransformerBuildItem(
                new AnnotationsTransformer() {
                    @Override
                    public boolean appliesTo(AnnotationTarget.Kind kind) {
                        return kind == AnnotationTarget.Kind.CLASS;
                    }

                    @Override
                    public void transform(AnnotationsTransformer.TransformationContext context) {
                        ClassInfo clazz = context.getTarget().asClass();
                        if (servicesToTransform.contains(clazz.name())) {
                            context.transform()
                                    .add(CollectStreams.class)
                                    .done();
                        }
                    }
                });
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public CardPageBuildItem pages(CombinedIndexBuildItem index) throws ClassNotFoundException,
            NoSuchMethodException,
            SecurityException,
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException,
            InvalidProtocolBufferException {

        // Create the card for Dev UI
        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .icon("font-awesome-solid:gears")
                .componentLink("qwc-grpc-services.js"));

        // Create gRPC Info
        Map<String, GrpcServiceClassInfo> m = createGrpcServiceClassInfos(getGrpcServices(index.getIndex()));
        DevConsoleManager.register("grpc-action", (params) -> {
            try {
                return grpcAction(params, m);
            } catch (InvalidProtocolBufferException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        });

        return cardPageBuildItem;
    }

    private Set<DotName> getRawTypesInHierarchy(ClassInfo clazz, IndexView index) {
        Set<DotName> rawTypes = new HashSet<>();
        addRawTypes(clazz, index, rawTypes);
        return rawTypes;
    }

    private void addRawTypes(ClassInfo clazz, IndexView index, Set<DotName> rawTypes) {
        rawTypes.add(clazz.name());
        for (DotName interfaceName : clazz.interfaceNames()) {
            rawTypes.add(interfaceName);
            ClassInfo interfaceClazz = index.getClassByName(interfaceName);
            if (interfaceClazz != null) {
                addRawTypes(interfaceClazz, index, rawTypes);
            }
        }
        if (clazz.superName() != null && !clazz.superName().equals(DotNames.OBJECT)) {
            ClassInfo superClazz = index.getClassByName(clazz.superName());
            if (superClazz != null) {
                addRawTypes(superClazz, index, rawTypes);
            }
        }
    }

    private ClassInfo findAbstractBindableService(ClassInfo clazz, IndexView index) {
        if (clazz.interfaceNames().contains(GrpcDotNames.BINDABLE_SERVICE)) {
            return clazz;
        }
        if (clazz.superName() != null && !clazz.superName().equals(DotNames.OBJECT)) {
            ClassInfo superClazz = index.getClassByName(clazz.superName());
            if (superClazz != null) {
                return findAbstractBindableService(superClazz, index);
            }
        }
        return null;
    }

    /**
     * This gets called during runtime from the Dev UI JsonRPC Service to test a grpc call
     * We go to Flow to stay in the JDK, else we have classpath issues.
     */
    private Flow.Publisher<String> grpcAction(Map<String, String> params, Map<String, GrpcServiceClassInfo> grpcClientStubs)
            throws InvalidProtocolBufferException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {

        final String serviceName = params.get("serviceName");
        final String methodName = params.get("methodName");
        final String methodType = params.get("methodType");
        final String content = params.get("content");
        final String host = params.get("host");
        final int port = Integer.valueOf(params.get("port"));

        BroadcastProcessor<String> streamEvent = BroadcastProcessor.create();

        GrpcServiceClassInfo info = grpcClientStubs.get(serviceName);

        Object grpcStub = createStub(info.grpcServiceClass, host, port);

        ServiceDescriptor serviceDescriptor = info.serviceDescriptor;

        final MethodDescriptor<?, ?> methodDescriptor = getMethodDescriptor(serviceDescriptor, methodName);
        MethodDescriptor.Marshaller<?> requestMarshaller = methodDescriptor.getRequestMarshaller();
        MethodDescriptor.PrototypeMarshaller<?> protoMarshaller = (MethodDescriptor.PrototypeMarshaller<?>) requestMarshaller;
        Class<?> requestType = protoMarshaller.getMessagePrototype().getClass();

        // Create a new builder for the request message, e.g. HelloRequest.newBuilder()
        Method newBuilderMethod = requestType.getDeclaredMethod("newBuilder");
        Message.Builder builder = (Message.Builder) newBuilderMethod.invoke(null);

        // Use the test data to build the request object
        JsonFormat.parser().merge(content, builder);
        Message message = builder.build();

        StreamObserver<?> responseObserver = new TestObserver<Object>(streamEvent);

        final Method stubMethod = getStubMethod(grpcStub, methodDescriptor.getBareMethodName());
        stubMethod.invoke(grpcStub, message, responseObserver);

        return streamEvent.convert().toPublisher();

    }

    @BuildStep(onlyIf = IsDevelopment.class)
    JsonRPCProvidersBuildItem createJsonRPCServiceForCache() {
        return new JsonRPCProvidersBuildItem(GrpcJsonRPCService.class);
    }

    private Map<String, GrpcServiceClassInfo> createGrpcServiceClassInfos(Collection<Class<?>> grpcServices) {
        Map<String, GrpcServiceClassInfo> m = new HashMap<>();

        for (Class<?> grpcServiceClass : grpcServices) {

            ServiceDescriptor serviceDescriptor = createServiceDescriptor(grpcServiceClass);
            GrpcServiceClassInfo s = new GrpcServiceClassInfo(serviceDescriptor, grpcServiceClass);
            m.put(serviceDescriptor.getName(), s);
        }
        return m;
    }

    private ServiceDescriptor createServiceDescriptor(Class<?> grpcServiceClass) {
        try {
            Method method = grpcServiceClass.getDeclaredMethod("getServiceDescriptor");
            return (ServiceDescriptor) method.invoke(null);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            log.warnf("Could not create stub for %s - " + e.getMessage(), grpcServiceClass);
            return null;
        }
    }

    private Object createStub(Class<?> grpcServiceClass, String host, int port) {
        try {
            Method stubFactoryMethod = grpcServiceClass.getDeclaredMethod("newStub", Channel.class);
            return stubFactoryMethod.invoke(null, getChannel(host, port));
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            log.warnf("Could not create stub for %s - " + e.getMessage(), grpcServiceClass);
            return null;
        }
    }

    private Channel getChannel(String host, int port) {
        return NettyChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
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

    private MethodDescriptor getMethodDescriptor(ServiceDescriptor serviceDescriptor, String methodName) {
        MethodDescriptor<?, ?> methodDescriptor = null;
        for (MethodDescriptor<?, ?> method : serviceDescriptor.getMethods()) {
            if (method.getBareMethodName() != null && method.getBareMethodName().equals(methodName)) {
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

    private Collection<Class<?>> getGrpcServices(IndexView index) throws ClassNotFoundException {
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
        serviceClasses.add(HealthGrpc.class);
        return serviceClasses;
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
                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(body);
                this.broadcaster.onNext(json.toPrettyString());
            } catch (InvalidProtocolBufferException | JsonProcessingException e) {
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

    static final class GrpcServiceClassInfo {
        public ServiceDescriptor serviceDescriptor;
        public Class<?> grpcServiceClass;

        public GrpcServiceClassInfo(ServiceDescriptor serviceDescriptor, Class<?> grpcServiceClass) {
            this.serviceDescriptor = serviceDescriptor;
            this.grpcServiceClass = grpcServiceClass;
        }
    }
}
