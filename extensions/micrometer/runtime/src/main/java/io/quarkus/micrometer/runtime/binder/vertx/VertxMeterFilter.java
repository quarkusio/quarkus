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
        RequestMetric requestMetric = RequestMetric.retrieveRequestMetric(context);

        if (requestMetric != null) {
            requestMetric.routingContext = routingContext;

            // remember if we can skip path munging --> @see VertxMeterBinderRestEasyContainerFilter
            if (requestMetric.pathMatched) {
                routingContext.put(RequestMetric.HTTP_REQUEST_PATH_MATCHED, true);
            }
        }
        routingContext.next();
    }

}
