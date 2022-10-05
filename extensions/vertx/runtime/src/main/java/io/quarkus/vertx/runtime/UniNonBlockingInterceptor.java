package io.quarkus.vertx.runtime;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;

@Interceptor
@UniNonBlocking
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class UniNonBlockingInterceptor {

    @Inject
    Vertx vertx;

    @AroundInvoke
    Object aroundInvoke(InvocationContext ctx) throws Exception {
        Context context = VertxContext.getOrCreateDuplicatedContext(vertx);
        VertxContextSafetyToggle.setContextSafe(context, true);
        return Uni.createFrom().emitter(em -> {
            context.runOnContext(v -> em.complete(proceedWithUni(ctx)));
        });
    }

    private Uni<?> proceedWithUni(InvocationContext ctx) {
        try {
            return (Uni<?>) ctx.proceed();
        } catch (Throwable t) {
            return Uni.createFrom().failure(t);
        }
    }

}
