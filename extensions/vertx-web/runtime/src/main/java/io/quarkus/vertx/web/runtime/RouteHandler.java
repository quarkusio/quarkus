package io.quarkus.vertx.web.runtime;

import io.quarkus.arc.runtime.BeanInvoker;
import io.quarkus.vertx.web.Route;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Handles invocation of a reactive route.
 * 
 * @see Route
 */
public interface RouteHandler extends Handler<RoutingContext>, BeanInvoker<RoutingContext> {

    @Override
    default void handle(RoutingContext context) {
        BeanInvoker.super.invoke(context);
    }

}
