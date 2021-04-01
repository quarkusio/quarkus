package io.quarkus.micrometer.runtime.binder.vertx;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

/**
 * High priority handler that sets the Vertx RouterContext
 * attribute on the active RequestMetric.
 * To quote Stuart, "YUCK".
 * Reference: https://github.com/eclipse-vertx/vert.x/issues/3579
 */
public class VertxMeterFilter implements Handler<RoutingContext> {
    @Override
    public void handle(RoutingContext routingContext) {
        final Context context = Vertx.currentContext();
        VertxHttpServerMetrics.moveRequestMetric(context, routingContext);
        routingContext.next();
    }
}
