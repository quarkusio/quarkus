package io.quarkus.vertx.deployment;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanClassAnnotationExclusion;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AnnotationProxyBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.IOThreadDetectorBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateConfigBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.netty.deployment.EventLoopSupplierBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.ConsumeEvent;
import io.quarkus.vertx.runtime.EventConsumerInvoker;
import io.quarkus.vertx.runtime.GenericMessageCodec;
import io.quarkus.vertx.runtime.VertxConfiguration;
import io.quarkus.vertx.runtime.VertxProducer;
import io.quarkus.vertx.runtime.VertxRecorder;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;

class VertxProcessor {

    private static final Logger LOGGER = Logger.getLogger(VertxProcessor.class.getName());

    private static final DotName CONSUME_EVENT = DotName.createSimple(ConsumeEvent.class.getName());
    private static final DotName MESSAGE = DotName.createSimple(Message.class.getName());
    private static final DotName RX_MESSAGE = DotName.createSimple(io.vertx.reactivex.core.eventbus.Message.class.getName());
    private static final DotName AXLE_MESSAGE = DotName.createSimple(io.vertx.axle.core.eventbus.Message.class.getName());
    private static final DotName COMPLETION_STAGE = DotName.createSimple(CompletionStage.class.getName());
    private static final DotName GENERIC_MESSAGE_CODEC = DotName.createSimple(GenericMessageCodec.class.getName());

    private static final String INVOKER_SUFFIX = "_VertxInvoker";

    private static final MethodDescriptor ARC_CONTAINER = MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class);
    private static final MethodDescriptor INSTANCE_HANDLE_GET = MethodDescriptor.ofMethod(InstanceHandle.class, "get",
            Object.class);
    private static final MethodDescriptor ARC_CONTAINER_BEAN = MethodDescriptor.ofMethod(ArcContainer.class, "bean",
            InjectableBean.class, String.class);
    private static final MethodDescriptor ARC_CONTAINER_INSTANCE_FOR_BEAN = MethodDescriptor.ofMethod(ArcContainer.class,
            "instance", InstanceHandle.class,
            InjectableBean.class);
    private static final MethodDescriptor ARC_CONTAINER_INSTANCE_FOR_TYPE = MethodDescriptor.ofMethod(ArcContainer.class,
            "instance", InstanceHandle.class,
            Class.class, Annotation[].class);
    private static final MethodDescriptor VERTX_EXECUTE_BLOCKING = MethodDescriptor.ofMethod(Vertx.class,
            "executeBlocking", void.class, Handler.class, boolean.class, Handler.class);
    private static final MethodDescriptor FUTURE_COMPLETE = MethodDescriptor.ofMethod(Future.class,
            "complete", void.class, Object.class);
    private static final MethodDescriptor FUTURE_FAIL = MethodDescriptor.ofMethod(Future.class,
            "fail", void.class, Throwable.class);
    private static final MethodDescriptor RX_MESSAGE_NEW_INSTANCE = MethodDescriptor.ofMethod(
            io.vertx.reactivex.core.eventbus.Message.class,
            "newInstance", io.vertx.reactivex.core.eventbus.Message.class, Message.class);
    private static final MethodDescriptor AXLE_MESSAGE_NEW_INSTANCE = MethodDescriptor.ofMethod(
            io.vertx.axle.core.eventbus.Message.class,
            "newInstance", io.vertx.axle.core.eventbus.Message.class, Message.class);
    private static final MethodDescriptor MESSAGE_REPLY = MethodDescriptor.ofMethod(Message.class, "reply", void.class,
            Object.class);
    private static final MethodDescriptor MESSAGE_BODY = MethodDescriptor.ofMethod(Message.class, "body", Object.class);
    private static final MethodDescriptor INSTANCE_HANDLE_DESTROY = MethodDescriptor.ofMethod(InstanceHandle.class, "destroy",
            void.class);

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @BuildStep
    SubstrateConfigBuildItem build() {
        return SubstrateConfigBuildItem.builder()
                .addNativeImageSystemProperty("vertx.disableDnsResolver", "true")
                .addRuntimeInitializedClass("io.vertx.core.http.impl.VertxHttp2ClientUpgradeCodec")
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem registerBean() {
        return AdditionalBeanBuildItem.unremovableOf(VertxProducer.class);
    }

    @Record(ExecutionTime.STATIC_INIT)
    EventLoopSupplierBuildItem eventLoop(VertxRecorder recorder) {
        return new EventLoopSupplierBuildItem(recorder.mainSupplier(), recorder.bossSupplier());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    VertxBuildItem build(VertxRecorder recorder, BeanContainerBuildItem beanContainer, BuildProducer<FeatureBuildItem> feature,
            List<EventConsumerBusinessMethodItem> messageConsumerBusinessMethods,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            AnnotationProxyBuildItem annotationProxy, LaunchModeBuildItem launchMode, ShutdownContextBuildItem shutdown,
            VertxConfiguration config, BuildProducer<ServiceStartBuildItem> serviceStart,
            List<MessageCodecBuildItem> messageCodecs, RecorderContext recorderContext) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.VERTX));
        Map<String, ConsumeEvent> messageConsumerConfigurations = new HashMap<>();
        ClassOutput classOutput = new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                generatedClass.produce(new GeneratedClassBuildItem(true, name, data));
            }
        };
        for (EventConsumerBusinessMethodItem businessMethod : messageConsumerBusinessMethods) {
            String invokerClass = generateInvoker(businessMethod.getBean(), businessMethod.getMethod(),
                    businessMethod.getConsumeEvent(), classOutput);
            messageConsumerConfigurations.put(invokerClass,
                    annotationProxy.builder(businessMethod.getConsumeEvent(), ConsumeEvent.class)
                            .withDefaultValue("value", businessMethod.getBean().getBeanClass().toString()).build(classOutput));
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, invokerClass));
        }

        Map<Class<?>, Class<?>> codecByClass = new HashMap<>();
        for (MessageCodecBuildItem messageCodecItem : messageCodecs) {
            codecByClass.put(recorderContext.classProxy(messageCodecItem.getType()),
                    recorderContext.classProxy(messageCodecItem.getCodec()));
        }

        RuntimeValue<Vertx> vertx = recorder.configureVertx(beanContainer.getValue(), config, messageConsumerConfigurations,
                launchMode.getLaunchMode(),
                shutdown, codecByClass);
        serviceStart.produce(new ServiceStartBuildItem("vertx"));
        return new VertxBuildItem(vertx);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    IOThreadDetectorBuildItem ioThreadDetector(VertxRecorder recorder) {
        return new IOThreadDetectorBuildItem(recorder.detector());
    }

    @BuildStep
    public UnremovableBeanBuildItem unremovableBeans() {
        return new UnremovableBeanBuildItem(new BeanClassAnnotationExclusion(CONSUME_EVENT));
    }

    @BuildStep
    void validateBeanDeployment(
            ValidationPhaseBuildItem validationPhase,
            BuildProducer<EventConsumerBusinessMethodItem> messageConsumerBusinessMethods,
            BuildProducer<ValidationErrorBuildItem> errors) {

        // We need to collect all business methods annotated with @MessageConsumer first
        AnnotationStore annotationStore = validationPhase.getContext().get(BuildExtension.Key.ANNOTATION_STORE);
        for (BeanInfo bean : validationPhase.getContext().get(BuildExtension.Key.BEANS)) {
            if (bean.isClassBean()) {
                // TODO: inherited business methods?
                for (MethodInfo method : bean.getTarget().get().asClass().methods()) {
                    AnnotationInstance consumeEvent = annotationStore.getAnnotation(method, CONSUME_EVENT);
                    if (consumeEvent != null) {
                        // Validate method params and return type
                        List<Type> params = method.parameters();
                        if (params.size() != 1) {
                            throw new IllegalStateException(String.format(
                                    "Event consumer business method must accept exactly one parameter: %s [method: %s, bean:%s",
                                    params, method, bean));
                        }
                        messageConsumerBusinessMethods
                                .produce(new EventConsumerBusinessMethodItem(bean, method, consumeEvent));
                        LOGGER.debugf("Found event consumer business method %s declared on %s", method, bean);
                    }
                }
            }
        }
    }

    @BuildStep
    AnnotationsTransformerBuildItem annotationTransformer() {
        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(org.jboss.jandex.AnnotationTarget.Kind kind) {
                return kind == org.jboss.jandex.AnnotationTarget.Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext context) {
                if (context.getAnnotations().isEmpty()) {
                    // Class with no annotations but with a method annotated with @ConsumeMessage
                    if (context.getTarget().asClass().annotations().containsKey(CONSUME_EVENT)) {
                        LOGGER.debugf(
                                "Found event consumer business methods on a class %s with no scope annotation - adding @Singleton",
                                context.getTarget());
                        context.transform().add(Singleton.class).done();
                    }
                }
            }
        });
    }

    private String generateInvoker(BeanInfo bean, MethodInfo method, AnnotationInstance consumeEvent, ClassOutput classOutput) {

        String baseName;
        if (bean.getImplClazz().enclosingClass() != null) {
            baseName = DotNames.simpleName(bean.getImplClazz().enclosingClass()) + "_"
                    + DotNames.simpleName(bean.getImplClazz());
        } else {
            baseName = DotNames.simpleName(bean.getImplClazz().name());
        }
        String targetPackage = DotNames.packageName(bean.getImplClazz().name());

        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(method.name()).append("_").append(method.returnType().name().toString());
        for (Type i : method.parameters()) {
            sigBuilder.append(i.name().toString());
        }
        String generatedName = targetPackage.replace('.', '/') + "/" + baseName + INVOKER_SUFFIX + "_" + method.name() + "_"
                + HashUtil.sha1(sigBuilder.toString());

        ClassCreator invokerCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(EventConsumerInvoker.class).build();

        MethodCreator invoke = invokerCreator.getMethodCreator("invoke", void.class, Message.class);
        ResultHandle containerHandle = invoke.invokeStaticMethod(ARC_CONTAINER);

        AnnotationValue blocking = consumeEvent.value("blocking");
        if (blocking != null && blocking.asBoolean()) {
            // Blocking operation must be performed on a worker thread
            ResultHandle vertxHandle = invoke
                    .invokeInterfaceMethod(INSTANCE_HANDLE_GET,
                            invoke.invokeInterfaceMethod(ARC_CONTAINER_INSTANCE_FOR_TYPE, containerHandle,
                                    invoke.loadClass(Vertx.class),
                                    invoke.newArray(Annotation.class.getName(), invoke.load(0))));

            FunctionCreator func = invoke.createFunction(Handler.class);
            BytecodeCreator funcBytecode = func.getBytecode();
            AssignableResultHandle messageHandle = funcBytecode.createVariable(Message.class);
            funcBytecode.assign(messageHandle, invoke.getMethodParam(0));
            TryBlock tryBlock = funcBytecode.tryBlock();
            invoke(bean, method, messageHandle, tryBlock);
            tryBlock.invokeInterfaceMethod(FUTURE_COMPLETE, funcBytecode.getMethodParam(0), tryBlock.loadNull());
            CatchBlockCreator catchBlock = tryBlock.addCatch(Exception.class);
            catchBlock.invokeInterfaceMethod(FUTURE_FAIL, funcBytecode.getMethodParam(0), catchBlock.getMethodParam(0));
            funcBytecode.returnValue(null);

            invoke.invokeInterfaceMethod(VERTX_EXECUTE_BLOCKING, vertxHandle, func.getInstance(), invoke.load(false),
                    invoke.loadNull());
        } else {
            invoke(bean, method, invoke.getMethodParam(0), invoke);
        }
        invoke.returnValue(null);
        invokerCreator.close();
        return generatedName.replace('/', '.');
    }

    private void invoke(BeanInfo bean, MethodInfo method, ResultHandle messageHandle, BytecodeCreator invoke) {
        ResultHandle containerHandle = invoke.invokeStaticMethod(ARC_CONTAINER);
        ResultHandle beanHandle = invoke.invokeInterfaceMethod(ARC_CONTAINER_BEAN, containerHandle,
                invoke.load(bean.getIdentifier()));
        ResultHandle instanceHandle = invoke.invokeInterfaceMethod(ARC_CONTAINER_INSTANCE_FOR_BEAN, containerHandle,
                beanHandle);
        ResultHandle beanInstanceHandle = invoke
                .invokeInterfaceMethod(INSTANCE_HANDLE_GET, instanceHandle);

        Type paramType = method.parameters().get(0);
        if (paramType.name().equals(MESSAGE)) {
            // io.vertx.core.eventbus.Message
            invoke.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(bean.getImplClazz().name().toString(), method.name(), void.class, Message.class),
                    beanInstanceHandle, messageHandle);
        } else if (paramType.name().equals(RX_MESSAGE)) {
            // io.vertx.reactivex.core.eventbus.Message
            ResultHandle rxMessageHandle = invoke.invokeStaticMethod(RX_MESSAGE_NEW_INSTANCE, messageHandle);
            invoke.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(bean.getImplClazz().name().toString(), method.name(), void.class,
                            io.vertx.reactivex.core.eventbus.Message.class),
                    beanInstanceHandle, rxMessageHandle);
        } else if (paramType.name().equals(AXLE_MESSAGE)) {
            // io.vertx.axle.core.eventbus.Message
            ResultHandle axleMessageHandle = invoke.invokeStaticMethod(AXLE_MESSAGE_NEW_INSTANCE, messageHandle);
            invoke.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(bean.getImplClazz().name().toString(), method.name(), void.class,
                            io.vertx.axle.core.eventbus.Message.class),
                    beanInstanceHandle, axleMessageHandle);
        } else {
            // Parameter is payload
            ResultHandle bodyHandle = invoke.invokeInterfaceMethod(MESSAGE_BODY, messageHandle);
            ResultHandle replyHandle = invoke.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(bean.getImplClazz().name().toString(), method.name(),
                            method.returnType().name().toString(), paramType.name().toString()),
                    beanInstanceHandle, bodyHandle);
            if (replyHandle != null) {
                if (method.returnType().name().equals(COMPLETION_STAGE)) {
                    // If the return type is CompletionStage use thenAccept()
                    FunctionCreator func = invoke.createFunction(Consumer.class);
                    BytecodeCreator funcBytecode = func.getBytecode();
                    funcBytecode.invokeInterfaceMethod(
                            MESSAGE_REPLY,
                            messageHandle,
                            funcBytecode.getMethodParam(0));
                    funcBytecode.returnValue(null);
                    // returnValue.thenAccept(reply -> Message.reply(reply))
                    invoke.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(CompletionStage.class, "thenAccept", CompletionStage.class,
                                    Consumer.class),
                            replyHandle, func.getInstance());
                } else {
                    // Message.reply(returnValue)
                    invoke.invokeInterfaceMethod(MESSAGE_REPLY, messageHandle, replyHandle);
                }
            }
        }

        // handle.destroy() - destroy dependent instance afterwards
        if (BuiltinScope.DEPENDENT.is(bean.getScope())) {
            invoke.invokeInterfaceMethod(INSTANCE_HANDLE_DESTROY, instanceHandle);
        }
    }

    @BuildStep
    public void registerCodecs(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            BuildProducer<MessageCodecBuildItem> messageCodecs) {
        final IndexView index = beanArchiveIndexBuildItem.getIndex();
        Collection<AnnotationInstance> consumeEventAnnotationInstances = index.getAnnotations(CONSUME_EVENT);
        Map<Type, DotName> codecByTypes = new HashMap<>();
        for (AnnotationInstance consumeEventAnnotationInstance : consumeEventAnnotationInstances) {
            AnnotationTarget typeTarget = consumeEventAnnotationInstance.target();
            if (typeTarget.kind() != AnnotationTarget.Kind.METHOD) {
                continue;
            }

            Type returnType = typeTarget.asMethod().returnType();
            Type typeToAdd = null;
            if (returnType.kind() == Kind.CLASS) {
                typeToAdd = returnType;
            } else if (returnType.kind() == Kind.PARAMETERIZED_TYPE) {
                ParameterizedType returnedParamType = returnType.asParameterizedType();

                if (returnedParamType.name().equals(MESSAGE)) {
                    typeToAdd = returnedParamType.arguments().get(0);
                } else if (returnedParamType.name().equals(RX_MESSAGE)) {
                    typeToAdd = returnedParamType.arguments().get(0);
                } else if (returnedParamType.name().equals(AXLE_MESSAGE)) {
                    typeToAdd = returnedParamType.arguments().get(0);
                } else if (returnedParamType.name().equals(COMPLETION_STAGE)) {
                    typeToAdd = returnedParamType.arguments().get(0);
                } else {
                    typeToAdd = returnedParamType;
                }
            }
            if (typeToAdd == null) {
                LOGGER.warnf(
                        "Cannot load return type of the consumer event annoted method %s",
                        typeTarget);
                continue;
            }
            AnnotationValue codec = consumeEventAnnotationInstance.value("codec");
            if (codec != null && codec.asClass().kind() == Kind.CLASS) {
                codecByTypes.put(typeToAdd, codec.asClass().asClassType().name());
                continue;
            }

            if (!codecByTypes.containsKey(typeToAdd)) {
                codecByTypes.put(typeToAdd, GENERIC_MESSAGE_CODEC);
            }
        }
        Map<Class<?>, Class<?>> codecByClass = new HashMap<>();
        for (Map.Entry<Type, DotName> entry : codecByTypes.entrySet()) {
            messageCodecs.produce(new MessageCodecBuildItem(entry.getKey().toString(), entry.getValue().toString()));
        }

    }

}
