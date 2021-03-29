package io.quarkus.vertx.deployment;

import static io.quarkus.vertx.deployment.VertxConstants.COMPLETION_STAGE;
import static io.quarkus.vertx.deployment.VertxConstants.MESSAGE;
import static io.quarkus.vertx.deployment.VertxConstants.MUTINY_MESSAGE;
import static io.quarkus.vertx.deployment.VertxConstants.UNI;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.vertx.runtime.EventConsumerInvoker;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.vertx.core.eventbus.Message;

class EventBusConsumer {

    private static final String INVOKER_SUFFIX = "_VertxInvoker";

    private static final MethodDescriptor INVOKER_CONSTRUCTOR = MethodDescriptor
            .ofConstructor(EventConsumerInvoker.class);
    private static final MethodDescriptor ARC_CONTAINER = MethodDescriptor
            .ofMethod(Arc.class, "container", ArcContainer.class);
    private static final MethodDescriptor INSTANCE_HANDLE_GET = MethodDescriptor.ofMethod(InstanceHandle.class, "get",
            Object.class);
    private static final MethodDescriptor ARC_CONTAINER_BEAN = MethodDescriptor.ofMethod(ArcContainer.class, "bean",
            InjectableBean.class, String.class);
    private static final MethodDescriptor ARC_CONTAINER_INSTANCE_FOR_BEAN = MethodDescriptor
            .ofMethod(ArcContainer.class,
                    "instance", InstanceHandle.class,
                    InjectableBean.class);
    private static final MethodDescriptor MUTINY_MESSAGE_NEW_INSTANCE = MethodDescriptor.ofMethod(
            io.vertx.mutiny.core.eventbus.Message.class,
            "newInstance", io.vertx.mutiny.core.eventbus.Message.class, Message.class);
    private static final MethodDescriptor MESSAGE_BODY = MethodDescriptor.ofMethod(Message.class, "body", Object.class);
    private static final MethodDescriptor INSTANCE_HANDLE_DESTROY = MethodDescriptor
            .ofMethod(InstanceHandle.class, "destroy",
                    void.class);
    protected static final MethodDescriptor WHEN_COMPLETE = MethodDescriptor.ofMethod(CompletionStage.class,
            "whenComplete", CompletionStage.class, BiConsumer.class);
    protected static final MethodDescriptor SUBSCRIBE_AS_COMPLETION_STAGE = MethodDescriptor
            .ofMethod(Uni.class, "subscribeAsCompletionStage", CompletableFuture.class);
    protected static final MethodDescriptor THROWABLE_GET_MESSAGE = MethodDescriptor
            .ofMethod(Throwable.class, "getMessage", String.class);
    protected static final MethodDescriptor THROWABLE_TO_STRING = MethodDescriptor
            .ofMethod(Throwable.class, "toString", String.class);
    protected static final DotName BLOCKING = DotName.createSimple(Blocking.class.getName());

    static String generateInvoker(BeanInfo bean, MethodInfo method,
            AnnotationInstance consumeEvent,
            ClassOutput classOutput) {

        String baseName;
        if (bean.getImplClazz().enclosingClass() != null) {
            baseName = DotNames.simpleName(bean.getImplClazz().enclosingClass()) + "_"
                    + DotNames.simpleName(bean.getImplClazz());
        } else {
            baseName = DotNames.simpleName(bean.getImplClazz().name());
        }
        String targetPackage = DotNames.internalPackageNameWithTrailingSlash(bean.getImplClazz().name());

        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(method.name()).append("_").append(method.returnType().name().toString());
        for (Type i : method.parameters()) {
            sigBuilder.append(i.name().toString());
        }
        String generatedName = targetPackage + baseName + INVOKER_SUFFIX + "_" + method.name() + "_"
                + HashUtil.sha1(sigBuilder.toString());

        boolean blocking;
        AnnotationValue blockingValue = consumeEvent.value("blocking");
        blocking = method.hasAnnotation(BLOCKING) || (blockingValue != null && blockingValue.asBoolean());

        ClassCreator invokerCreator = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .superClass(EventConsumerInvoker.class).build();

        // Initialized state
        FieldCreator beanField = invokerCreator.getFieldCreator("bean", InjectableBean.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);
        FieldCreator containerField = invokerCreator.getFieldCreator("container", ArcContainer.class)
                .setModifiers(ACC_PRIVATE | ACC_FINAL);

        if (blocking) {
            MethodCreator isBlocking = invokerCreator.getMethodCreator("isBlocking", boolean.class);
            isBlocking.returnValue(isBlocking.load(true));
        }

        AnnotationValue orderedValue = consumeEvent.value("ordered");
        boolean ordered = orderedValue != null && orderedValue.asBoolean();
        if (ordered) {
            MethodCreator isOrdered = invokerCreator.getMethodCreator("isOrdered", boolean.class);
            isOrdered.returnValue(isOrdered.load(true));
        }

        implementConstructor(bean, invokerCreator, beanField, containerField);
        implementInvoke(bean, method, invokerCreator, beanField.getFieldDescriptor(), containerField.getFieldDescriptor());

        invokerCreator.close();
        return generatedName.replace('/', '.');
    }

    static void implementConstructor(BeanInfo bean, ClassCreator invokerCreator, FieldCreator beanField,
            FieldCreator containerField) {
        MethodCreator constructor = invokerCreator.getMethodCreator("<init>", void.class);
        // Invoke super()
        constructor.invokeSpecialMethod(INVOKER_CONSTRUCTOR, constructor.getThis());

        ResultHandle containerHandle = constructor
                .invokeStaticMethod(ARC_CONTAINER);
        ResultHandle beanHandle = constructor.invokeInterfaceMethod(
                ARC_CONTAINER_BEAN,
                containerHandle, constructor.load(bean.getIdentifier()));
        constructor.writeInstanceField(beanField.getFieldDescriptor(), constructor.getThis(), beanHandle);
        constructor.writeInstanceField(containerField.getFieldDescriptor(), constructor.getThis(), containerHandle);
        constructor.returnValue(null);
    }

    private static void implementInvoke(BeanInfo bean, MethodInfo method, ClassCreator invokerCreator,
            FieldDescriptor beanField,
            FieldDescriptor containerField) {

        // The method descriptor is: CompletionStage invokeBean(Message message)
        MethodCreator invoke = invokerCreator.getMethodCreator("invokeBean", Object.class, Message.class)
                .addException(Exception.class);

        ResultHandle containerHandle = invoke.readInstanceField(containerField, invoke.getThis());
        ResultHandle beanHandle = invoke.readInstanceField(beanField, invoke.getThis());
        ResultHandle instanceHandle = invoke.invokeInterfaceMethod(ARC_CONTAINER_INSTANCE_FOR_BEAN, containerHandle,
                beanHandle);
        ResultHandle beanInstanceHandle = invoke
                .invokeInterfaceMethod(INSTANCE_HANDLE_GET, instanceHandle);
        ResultHandle messageHandle = invoke.getMethodParam(0);
        ResultHandle result;

        Type paramType = method.parameters().get(0);
        if (paramType.name().equals(MESSAGE)) {
            // io.vertx.core.eventbus.Message
            invoke.invokeVirtualMethod(
                    MethodDescriptor
                            .ofMethod(bean.getImplClazz().name().toString(), method.name(), void.class, Message.class),
                    beanInstanceHandle, messageHandle);
            result = invoke.loadNull();
        } else if (paramType.name().equals(MUTINY_MESSAGE)) {
            // io.vertx.mutiny.core.eventbus.Message
            ResultHandle mutinyMessageHandle = invoke.invokeStaticMethod(MUTINY_MESSAGE_NEW_INSTANCE, messageHandle);
            invoke.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(bean.getImplClazz().name().toString(), method.name(), void.class,
                            io.vertx.mutiny.core.eventbus.Message.class),
                    beanInstanceHandle, mutinyMessageHandle);
            result = invoke.loadNull();
        } else {
            // Parameter is payload
            ResultHandle bodyHandle = invoke.invokeInterfaceMethod(MESSAGE_BODY, messageHandle);
            ResultHandle returnHandle = invoke.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(bean.getImplClazz().name().toString(), method.name(),
                            method.returnType().name().toString(), paramType.name().toString()),
                    beanInstanceHandle, bodyHandle);
            if (returnHandle != null) {
                if (method.returnType().name().equals(COMPLETION_STAGE)) {
                    result = returnHandle;
                } else if (method.returnType().name().equals(UNI)) {
                    result = invoke.invokeInterfaceMethod(SUBSCRIBE_AS_COMPLETION_STAGE,
                            returnHandle);
                } else {
                    // Message.reply(returnValue)
                    result = returnHandle;
                }
            } else {
                result = invoke.loadNull();
            }
        }

        // handle.destroy() - destroy dependent instance afterwards
        if (BuiltinScope.DEPENDENT.is(bean.getScope())) {
            invoke.invokeInterfaceMethod(INSTANCE_HANDLE_DESTROY, instanceHandle);
        }

        invoke.returnValue(result);
    }

    private EventBusConsumer() {
        // Avoid direct instantiation.
    }
}
