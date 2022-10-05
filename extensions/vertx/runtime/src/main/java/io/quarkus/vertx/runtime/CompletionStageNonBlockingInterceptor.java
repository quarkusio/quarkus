package io.quarkus.vertx.runtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

@Interceptor
@CompletionStageNonBlocking
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class CompletionStageNonBlockingInterceptor {

    @Inject
    Vertx vertx;

    @AroundInvoke
    Object aroundInvoke(InvocationContext ctx) throws Exception {
        Context context = VertxContext.getOrCreateDuplicatedContext(vertx);
        VertxContextSafetyToggle.setContextSafe(context, true);
        CompletableFuture<Object> cf = new CompletableFuture<>();
        context.runOnContext(v -> proceedWithStage(ctx).whenComplete((r, t) -> {
            if (t != null) {
                cf.completeExceptionally(t);
            } else {
                cf.complete(r);
            }
        }));
        return cf;
    }

    private CompletionStage<?> proceedWithStage(InvocationContext ctx) {
        try {
            return (CompletionStage<?>) ctx.proceed();
        } catch (Throwable t) {
            return CompletableFuture.failedStage(t);
        }
    }
}
