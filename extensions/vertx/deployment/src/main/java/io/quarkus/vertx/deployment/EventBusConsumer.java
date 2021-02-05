package io.quarkus.vertx.deployment;

import static io.quarkus.vertx.deployment.VertxConstants.COMPLETION_STAGE;
import static io.quarkus.vertx.deployment.VertxConstants.MESSAGE;
import static io.quarkus.vertx.deployment.VertxConstants.MUTINY_MESSAGE;
import static io.quarkus.vertx.deployment.VertxConstants.UNI;

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
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.util.HashUtil;
import io.quarkus.vertx.ConsumeEvent;
import io.quarkus.vertx.runtime.EventConsumerInvoker;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.vertx.core.eventbus.Message;

class EventBusConsumer {

    private static final String INVOKER_SUFFIX = "_VertxInvoker";

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
    private static final MethodDescriptor MESSAGE_REPLY = MethodDescriptor.ofMethod(Message.class, "reply", void.class,
            Object.class);
    private static final MethodDescriptor MESSAGE_FAIL = MethodDescriptor.ofMethod(Message.class, "fail", void.class,
            Integer.TYPE, String.class);
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
                .interfaces(EventConsumerInvoker.class).build();

        // The method descriptor is: void invokeBean(Object message)
        MethodCreator invoke = invokerCreator.getMethodCreator("invokeBean", void.class, Object.class)
                .addException(Exception.class);

        if (blocking) {
            MethodCreator isBlocking = invokerCreator.getMethodCreator("isBlocking", boolean.class);
            isBlocking.returnValue(isBlocking.load(true));
        }

        invoke(bean, method, invoke.getMethodParam(0), invoke);

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
        } else if (paramType.name().equals(MUTINY_MESSAGE)) {
            // io.vertx.mutiny.core.eventbus.Message
            ResultHandle mutinyMessageHandle = invoke.invokeStaticMethod(MUTINY_MESSAGE_NEW_INSTANCE, messageHandle);
            invoke.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(bean.getImplClazz().name().toString(), method.name(), void.class,
                            io.vertx.mutiny.core.eventbus.Message.class),
                    beanInstanceHandle, mutinyMessageHandle);
        } else {
            // Parameter is payload
            ResultHandle bodyHandle = invoke.invokeInterfaceMethod(MESSAGE_BODY, messageHandle);
            ResultHandle replyHandle = invoke.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(bean.getImplClazz().name().toString(), method.name(),
                            method.returnType().name().toString(), paramType.name().toString()),
                    beanInstanceHandle, bodyHandle);
            if (replyHandle != null) {
                if (method.returnType().name().equals(COMPLETION_STAGE)) {
                    FunctionCreator handler = generateWhenCompleteHandler(messageHandle, invoke);
                    invoke.invokeInterfaceMethod(
                            WHEN_COMPLETE,
                            replyHandle, handler.getInstance());
                } else if (method.returnType().name().equals(UNI)) {
                    // If the return type is Uni use uni.subscribeAsCompletionStage().whenComplete(...)
                    FunctionCreator handler = generateWhenCompleteHandler(messageHandle, invoke);
                    ResultHandle subscribedCompletionStage = invoke.invokeInterfaceMethod(SUBSCRIBE_AS_COMPLETION_STAGE,
                            replyHandle);
                    invoke.invokeInterfaceMethod(WHEN_COMPLETE,
                            subscribedCompletionStage, handler.getInstance());
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

    /**
     * If the return type is CompletionStage use:
     * <code><pre>
     * cs.whenComplete((whenResult, whenFailure) -> {
     *  if (failure != null) {
     *         message.fail(status, whenFailure.getMessage());
     *  } else {
     *         message.reply(whenResult);
     *  }
     * })
     * </pre></code>
     *
     * @param messageHandle the message variable
     * @param invoke the bytecode creator
     * @return the function
     */
    private static FunctionCreator generateWhenCompleteHandler(ResultHandle messageHandle, BytecodeCreator invoke) {
        FunctionCreator handler = invoke.createFunction(BiConsumer.class);
        BytecodeCreator bytecode = handler.getBytecode();

        // This avoid having to check cast in the branches
        AssignableResultHandle whenResult = bytecode.createVariable(Object.class);
        bytecode.assign(whenResult, bytecode.getMethodParam(0));
        AssignableResultHandle whenFailure = bytecode.createVariable(Exception.class);
        bytecode.assign(whenFailure, bytecode.getMethodParam(1));
        AssignableResultHandle message = bytecode.createVariable(Message.class);
        bytecode.assign(message, messageHandle);

        BranchResult ifFailureIfNull = bytecode.ifNull(whenFailure);
        // failure is not null branch - message.fail(failureStatus, failure.getMessage())
        // In this branch we use the EXPLICIT FAILURE CODE
        BytecodeCreator failureIsNotNull = ifFailureIfNull.falseBranch();
        ResultHandle failureStatus = failureIsNotNull.load(ConsumeEvent.EXPLICIT_FAILURE_CODE);
        ResultHandle failureMessage = failureIsNotNull
                .invokeVirtualMethod(THROWABLE_GET_MESSAGE, whenFailure);
        failureIsNotNull.invokeInterfaceMethod(
                MESSAGE_FAIL,
                message,
                failureStatus,
                failureMessage);

        // failure is null branch - message.reply(reply))
        BytecodeCreator failureIsNull = ifFailureIfNull.trueBranch();
        failureIsNull.invokeInterfaceMethod(
                MESSAGE_REPLY,
                messageHandle,
                whenResult);

        bytecode.returnValue(null);
        return handler;
    }

    private EventBusConsumer() {
        // Avoid direct instantiation.
    }
}
