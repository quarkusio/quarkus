package io.quarkus.micrometer.runtime.binder.vertx;

import org.jboss.logging.Logger;

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
    private static final Logger log = Logger.getLogger(VertxMeterFilter.class);

    @Override
    public void handle(RoutingContext event) {
        final Context context = Vertx.currentContext();
        log.debugf("Handling event %s with context %s", event, context);

        RequestMetric requestMetric = RequestMetric.retrieveRequestMetric(context);
        if (requestMetric != null) {
            requestMetric.routingContext = event;
        }
        event.next();
    }
}
