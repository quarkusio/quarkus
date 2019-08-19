package io.quarkus.vertx.web.deployment;

import io.quarkus.builder.item.MultiBuildItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * A handler that is applied to every route
 */
public final class FilterBuildItem extends MultiBuildItem {

    final Handler<RoutingContext> handler;

    public FilterBuildItem(Handler<RoutingContext> handler) {
        this.handler = handler;
    }

    public Handler<RoutingContext> getHandler() {
        return handler;
    }
}
