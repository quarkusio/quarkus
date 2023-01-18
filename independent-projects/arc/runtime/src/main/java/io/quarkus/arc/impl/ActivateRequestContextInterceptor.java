package io.quarkus.arc.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Interceptor
@ActivateRequestContext
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 100)
public class ActivateRequestContextInterceptor {

    @AroundInvoke
    Object aroundInvoke(InvocationContext ctx) throws Exception {
        switch (ReactiveType.valueOf(ctx.getMethod())) {
            case UNI:
                return invokeUni(ctx);
            case MULTI:
                return invokeMulti(ctx);
            case STAGE:
                return invokeStage(ctx);
            default:
                return invoke(ctx);
        }
    }

    private CompletionStage<?> invokeStage(InvocationContext ctx) {
        ManagedContext requestContext = Arc.container().requestContext();
        if (requestContext.isActive()) {
            return proceedWithStage(ctx);
        }

        return activate(requestContext)
                .thenCompose(state -> proceedWithStage(ctx).whenComplete((r, t) -> {
                    requestContext.destroy(state);
                    requestContext.deactivate();
                }));
    }

    private static CompletionStage<InjectableContext.ContextState> activate(ManagedContext requestContext) {
        try {
            requestContext.activate();
            return CompletableFuture.completedStage(requestContext.getState());
        } catch (Throwable t) {
            return CompletableFuture.failedStage(t);
        }
    }

    private CompletionStage<?> proceedWithStage(InvocationContext ctx) {
        try {
            return (CompletionStage<?>) ctx.proceed();
        } catch (Throwable t) {
            return CompletableFuture.failedStage(t);
        }
    }

    private Multi<?> invokeMulti(InvocationContext ctx) {
        return Multi.createFrom().deferred(() -> {
            ManagedContext requestContext = Arc.container().requestContext();
            if (requestContext.isActive()) {
                return proceedWithMulti(ctx);
            }

            return Multi.createFrom().deferred(() -> {
                requestContext.activate();
                InjectableContext.ContextState state = requestContext.getState();
                return proceedWithMulti(ctx)
                        .onTermination().invoke(() -> {
                            requestContext.destroy(state);
                            requestContext.deactivate();
                        });
            });
        });
    }

    private Multi<?> proceedWithMulti(InvocationContext ctx) {
        try {
            return (Multi<?>) ctx.proceed();
        } catch (Throwable t) {
            return Multi.createFrom().failure(t);
        }
    }

    private Uni<?> invokeUni(InvocationContext ctx) {
        return Uni.createFrom().deferred(() -> {
            ManagedContext requestContext = Arc.container().requestContext();
            if (requestContext.isActive()) {
                return proceedWithUni(ctx);
            }

            return Uni.createFrom().deferred(() -> {
                requestContext.activate();
                InjectableContext.ContextState state = requestContext.getState();
                return proceedWithUni(ctx)
                        .eventually(() -> {
                            requestContext.destroy(state);
                            requestContext.deactivate();
                        });
            });
        });
    }

    private Uni<?> proceedWithUni(InvocationContext ctx) {
        try {
            return (Uni<?>) ctx.proceed();
        } catch (Throwable t) {
            return Uni.createFrom().failure(t);
        }
    }

    private Object invoke(InvocationContext ctx) throws Exception {
        ManagedContext requestContext = Arc.container().requestContext();
        if (requestContext.isActive()) {
            return ctx.proceed();
        }

        try {
            requestContext.activate();
            return ctx.proceed();
        } finally {
            requestContext.terminate();
        }
    }
}
