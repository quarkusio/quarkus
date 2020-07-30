package io.quarkus.micrometer.runtime.binder.vertx;

import org.jboss.logging.Logger;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

public class VertxMeterFilter implements Handler<RoutingContext> {
    private static final Logger log = Logger.getLogger(VertxMeterFilter.class);

    @Override
    public void handle(RoutingContext event) {
        final Context context = Vertx.currentContext();
        log.debugf("Handling event %s with context %s", event, context);

        MetricsContext.addRoutingContext(context, event);
        event.addBodyEndHandler(new Handler<Void>() {
            @Override
            public void handle(Void x) {
                MetricsContext.removeRoutingContext(context);
            }
        });

        event.next();
    }
}
