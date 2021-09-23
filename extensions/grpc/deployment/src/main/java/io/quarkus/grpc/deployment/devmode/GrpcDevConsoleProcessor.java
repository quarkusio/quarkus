package io.quarkus.grpc.deployment.devmode;

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

import javax.inject.Singleton;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

import grpc.health.v1.HealthGrpc;
import io.grpc.MethodDescriptor;
import io.grpc.MethodDescriptor.Marshaller;
import io.grpc.MethodDescriptor.PrototypeMarshaller;
import io.grpc.ServiceDescriptor;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.arc.runtime.BeanLookupSupplier;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.dev.testing.GrpcWebSocketProxy;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.grpc.deployment.DelegatingGrpcBeanBuildItem;
import io.quarkus.grpc.deployment.GrpcDotNames;
import io.quarkus.grpc.protoc.plugin.MutinyGrpcGenerator;
import io.quarkus.grpc.runtime.devmode.DelegatingGrpcBeansStorage;
import io.quarkus.grpc.runtime.devmode.GrpcDevConsoleRecorder;
import io.quarkus.grpc.runtime.devmode.GrpcServices;
import io.quarkus.vertx.http.deployment.spi.NonApplicationRootPathConfigurerBuildItem;

public class GrpcDevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public void devConsoleInfo(BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<DevConsoleRuntimeTemplateInfoBuildItem> infos) {
        beans.produce(AdditionalBeanBuildItem.unremovableOf(GrpcServices.class));
        infos.produce(
                new DevConsoleRuntimeTemplateInfoBuildItem("grpcServices",
                        new BeanLookupSupplier(GrpcServices.class)));
    }

    @IfBuildProfile("dev")
    @BuildStep
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

        GrpcWebSocketProxy.setWebSocketListener(
                new GrpcDevConsoleWebSocketListener(grpcServices, Thread.currentThread().getContextClassLoader()));
    }

    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    public void createWebSocketEndpoint(BuildProducer<NonApplicationRootPathConfigurerBuildItem> configurers,
            GrpcDevConsoleRecorder recorder) {
        recorder.setServerConfiguration();

        configurers.produce(new NonApplicationRootPathConfigurerBuildItem(
                builder -> builder.route("dev/grpc-test").handler(recorder.handler())));
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
        serviceClasses.add(HealthGrpc.class);
        return serviceClasses;
    }
}
