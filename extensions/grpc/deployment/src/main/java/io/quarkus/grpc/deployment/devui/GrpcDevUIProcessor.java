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

import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

import grpc.health.v1.HealthGrpc;
import io.grpc.MethodDescriptor;
import io.grpc.ServiceDescriptor;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.IsLocalDevelopment;
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
import io.quarkus.grpc.runtime.dev.ui.GrpcJsonRPCService;
import io.quarkus.grpc.runtime.devmode.CollectStreams;
import io.quarkus.grpc.runtime.devmode.DelegatingGrpcBeansStorage;
import io.quarkus.grpc.runtime.devmode.GrpcServices;
import io.quarkus.grpc.runtime.devmode.StreamCollectorInterceptor;

public class GrpcDevUIProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    public AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(GrpcServices.class)
                .addBeanClasses(StreamCollectorInterceptor.class, CollectStreams.class)
                .build();
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
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

    @BuildStep(onlyIf = IsLocalDevelopment.class)
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

    @BuildStep(onlyIf = IsLocalDevelopment.class)
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

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    public CardPageBuildItem pages(CombinedIndexBuildItem index) throws ClassNotFoundException,
            NoSuchMethodException,
            SecurityException,
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException,
            InvalidProtocolBufferException {

        // Create the card for Dev UI
        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();
        cardPageBuildItem.setLogo("gRPC.png", "gRPC.png");
        cardPageBuildItem.addLibraryVersion("io.grpc", "grpc-api", "gRPC-Java", "https://github.com/grpc/grpc-java");

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .icon("font-awesome-solid:gears")
                .componentLink("qwc-grpc-services.js"));

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

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    JsonRPCProvidersBuildItem createJsonRPCServiceForCache() {
        return new JsonRPCProvidersBuildItem(GrpcJsonRPCService.class);
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

        serviceClassNames.add(HealthGrpc.class.getName());
        DevConsoleManager.setGlobal("io.quarkus.grpc.serviceClassNames", serviceClassNames);

        List<Class<?>> serviceClasses = new ArrayList<>();
        for (String className : serviceClassNames) {
            serviceClasses.add(tccl.loadClass(className));
        }
        return serviceClasses;
    }
}
