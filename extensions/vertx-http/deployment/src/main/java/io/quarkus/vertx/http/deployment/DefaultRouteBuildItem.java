package io.quarkus.vertx.http.deployment;

import java.util.function.Consumer;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.vertx.http.runtime.HandlerConsumer;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

/**
 * A build item that represents a handler for the default route
 */
public final class DefaultRouteBuildItem extends MultiBuildItem {

    private final Consumer<Route> route;

    public DefaultRouteBuildItem(Handler<RoutingContext> handler) {
        HandlerConsumer route = new HandlerConsumer();
        route.setHandler(handler);
        this.route = route;
    }

    public DefaultRouteBuildItem(Consumer<Route> route) {
        this.route = route;
    }

    public Consumer<Route> getRoute() {
        return route;
    }

}
