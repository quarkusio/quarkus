package io.quarkus.signals.runtime.impl;

import jakarta.inject.Singleton;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.signals.spi.ReceiverInterceptor;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

/**
 * Built-in interceptor that activates a new CDI request context for each receiver invocation.
 */
@Identifier(ReceiverInterceptor.ID_REQUEST_CONTEXT)
@Singleton
public class VertxRequestContextInterceptor implements ReceiverInterceptor {

    private final ManagedContext requestContext;

    public VertxRequestContextInterceptor() {
        requestContext = Arc.container().requestContext();
    }

    @Override
    public Uni<Object> intercept(InterceptionContext context) {
        Context vertxContext = Vertx.currentContext();
        if (!VertxContext.isDuplicatedContext(vertxContext)) {
            throw new IllegalStateException("VertxRequestContextInterceptor may only be used with Vertx duplicated context");
        }
        requestContext.activate();
        return context.proceed().eventually(new Runnable() {
            @Override
            public void run() {
                vertxContext.runOnContext(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        requestContext.terminate();
                    }
                });
            }
        });
    }

}
