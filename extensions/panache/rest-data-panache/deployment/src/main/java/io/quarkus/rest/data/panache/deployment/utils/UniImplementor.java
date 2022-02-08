package io.quarkus.rest.data.panache.deployment.utils;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.data.panache.RestDataPanacheException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniCreate;
import io.smallrye.mutiny.groups.UniOnFailure;

public final class UniImplementor {

    /**
     * Implements Uni.createFrom().item(object) which returns an `Uni<Object>` instance.
     */
    public static ResultHandle createFrom(BytecodeCreator creator, ResultHandle item) {
        ResultHandle createFrom = creator.invokeStaticMethod(ofMethod(Uni.class, "createFrom", UniCreate.class));
        ResultHandle uniItem = creator.invokeVirtualMethod(ofMethod(UniCreate.class, "item", Uni.class, Object.class),
                createFrom, item);
        return uniItem;
    }

    /**
     * Given a Uni instance, this method implements the `map` method:
     * uni.map(item -> ...).
     */
    public static ResultHandle map(BytecodeCreator creator, ResultHandle uniInstance, String messageOnFailure,
            BiConsumer<BytecodeCreator, ResultHandle> function) {
        ResultHandle rrContext = creator
                .invokeStaticMethod(ofMethod(CurrentRequestManager.class, "get", ResteasyReactiveRequestContext.class));

        FunctionCreator lambda = creator.createFunction(Function.class);
        BytecodeCreator body = lambda.getBytecode();
        ResultHandle item = body.getMethodParam(0);
        body.invokeStaticMethod(ofMethod(CurrentRequestManager.class, "set", void.class, ResteasyReactiveRequestContext.class),
                rrContext);

        function.accept(body, item);

        ResultHandle resultHandle = creator.invokeInterfaceMethod(ofMethod(Uni.class, "map", Uni.class, Function.class),
                uniInstance, lambda.getInstance());

        return onFailure(creator, resultHandle, messageOnFailure);
    }

    /**
     * Given a Uni instance, this method implements the `flatMap` method:
     * uni.flatMap(item -> ...).
     */
    public static ResultHandle flatMap(BytecodeCreator creator, ResultHandle uniInstance, String messageOnFailure,
            BiConsumer<BytecodeCreator, ResultHandle> function) {
        ResultHandle rrContext = creator
                .invokeStaticMethod(ofMethod(CurrentRequestManager.class, "get", ResteasyReactiveRequestContext.class));
        FunctionCreator lambda = creator.createFunction(Function.class);
        BytecodeCreator body = lambda.getBytecode();
        ResultHandle item = body.getMethodParam(0);
        body.invokeStaticMethod(ofMethod(CurrentRequestManager.class, "set", void.class, ResteasyReactiveRequestContext.class),
                rrContext);
        function.accept(body, item);

        ResultHandle resultHandle = creator.invokeInterfaceMethod(ofMethod(Uni.class, "flatMap", Uni.class, Function.class),
                uniInstance, lambda.getInstance());

        return onFailure(creator, resultHandle, messageOnFailure);
    }

    /**
     * Given a Uni instance, this method implements the `onFailure` method:
     * uni.onFailure(ex -> ...).
     */
    public static ResultHandle onFailure(BytecodeCreator creator, ResultHandle uniInstance, String message) {
        return onFailure(creator, uniInstance,
                (body, ex) -> body.newInstance(
                        MethodDescriptor.ofConstructor(RestDataPanacheException.class, String.class, Throwable.class),
                        body.load(message), ex));
    }

    /**
     * Given a Uni instance, this method implements the `onFailure` method:
     * uni.onFailure().invoke(ex -> ...).
     */
    public static ResultHandle onFailure(BytecodeCreator creator, ResultHandle uniInstance,
            BiFunction<BytecodeCreator, ResultHandle, ResultHandle> exceptionHandler) {
        FunctionCreator lambda = creator.createFunction(Consumer.class);
        BytecodeCreator body = lambda.getBytecode();
        ResultHandle exception = body.checkCast(body.getMethodParam(0), Throwable.class);
        body.throwException(exceptionHandler.apply(body, exception));

        ResultHandle uniOnFailure = creator.invokeInterfaceMethod(ofMethod(Uni.class, "onFailure", UniOnFailure.class),
                uniInstance);

        return creator.invokeVirtualMethod(ofMethod(UniOnFailure.class, "invoke", Uni.class, Consumer.class),
                uniOnFailure, lambda.getInstance());
    }

}
