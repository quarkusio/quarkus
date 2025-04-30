package io.quarkus.vertx.web.runtime;

import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.quarkus.virtual.threads.VirtualThreadsRecorder;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class VirtualThreadsRouteHandler implements Handler<RoutingContext> {

    private final Handler<RoutingContext> routeHandler;

    public VirtualThreadsRouteHandler(Handler<RoutingContext> routeHandler) {
        this.routeHandler = routeHandler;
    }

    @Override
    public void handle(RoutingContext context) {
        Context vertxContext = VertxContext.getOrCreateDuplicatedContext(VertxCoreRecorder.getVertx().get());
        VertxContextSafetyToggle.setContextSafe(vertxContext, true);
        vertxContext.runOnContext(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                VirtualThreadsRecorder.getCurrent().execute(new Runnable() {
                    @Override
                    public void run() {
                        routeHandler.handle(context);
                    }
                });
            }
        });
    }

}
