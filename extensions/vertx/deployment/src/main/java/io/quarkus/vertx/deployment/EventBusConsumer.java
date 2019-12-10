package io.quarkus.vertx.deployment;

import static io.quarkus.vertx.deployment.VertxConstants.*;

import java.lang.annotation.Annotation;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.*;
import io.quarkus.vertx.runtime.EventConsumerInvoker;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;

class EventBusConsumer {
    private static final String INVOKER_SUFFIX = "_VertxInvoker";
    private static final MethodDescriptor ARC_CONTAINER = MethodDescriptor
            .ofMethod(Arc.class, "container", ArcContainer.class);
    private static final MethodDescriptor INSTANCE_HANDLE_GET = MethodDescriptor.ofMethod(InstanceHandle.class, "get",
            Object.class);
    private static final MethodDescriptor ARC_CONTAINER_INSTANCE_FOR_TYPE = MethodDescriptor
            .ofMethod(ArcContainer.class,
                    "instance", InstanceHandle.class,
                    Class.class, Annotation[].class);
    private static final MethodDescriptor VERTX_EXECUTE_BLOCKING = MethodDescriptor.ofMethod(Vertx.class,
            "executeBlocking", void.class, Handler.class, boolean.class, Handler.class);
    private static final MethodDescriptor FUTURE_COMPLETE = MethodDescriptor.ofMethod(Future.class,
            "complete", void.class, Object.class);
    private static final MethodDescriptor FUTURE_FAIL = MethodDescriptor.ofMethod(Future.class,
            "fail", void.class, Throwable.class);
    private static final MethodDescriptor ARC_CONTAINER_BEAN = MethodDescriptor.ofMethod(ArcContainer.class, "bean",
            InjectableBean.class, String.class);
    private static final MethodDescriptor ARC_CONTAINER_INSTANCE_FOR_BEAN = MethodDescriptor
            .ofMethod(ArcContainer.class,
                    "instance", InstanceHandle.class,
                    InjectableBean.class);
    private static final MethodDescriptor RX_MESSAGE_NEW_INSTANCE = MethodDescriptor.ofMethod(
            io.vertx.reactivex.core.eventbus.Message.class,
            "newInstance", io.vertx.reactivex.core.eventbus.Message.class, Message.class);
    private static final MethodDescriptor AXLE_MESSAGE_NEW_INSTANCE = MethodDescriptor.ofMethod(
            io.vertx.axle.core.eventbus.Message.class,
            "newInstance", io.vertx.axle.core.eventbus.Message.class, Message.class);
    private static final MethodDescriptor MESSAGE_REPLY = MethodDescriptor.ofMethod(Message.class, "reply", void.class,
            Object.class);
    private static final MethodDescriptor MESSAGE_BODY = MethodDescriptor.ofMethod(Message.class, "body", Object.class);
    private static final MethodDescriptor INSTANCE_HANDLE_DESTROY = MethodDescriptor
            .ofMethod(InstanceHandle.class, "destroy",
                    void.class);

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

        // The method descriptor is: void invokeBean(Object message)
        MethodCreator invoke = invokerCreator.getMethodCreator("invokeBean", void.class, Object.class);
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

    private static void invoke(BeanInfo bean, MethodInfo method, ResultHandle messageHandle, BytecodeCreator invoke) {
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
                    MethodDescriptor
                            .ofMethod(bean.getImplClazz().name().toString(), method.name(), void.class, Message.class),
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

    private EventBusConsumer() {
        // Avoid direct instantiation.
    }
}
