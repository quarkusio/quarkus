package io.quarkus.vertx.http.deployment;

import java.util.function.Function;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public final class RouteBuildItem extends MultiBuildItem {

    private final Function<Router, Route> routeFunction;
    private final Handler<RoutingContext> handler;
    private final HandlerType type;

    public RouteBuildItem(Function<Router, Route> routeFunction, Handler<RoutingContext> handler, HandlerType type) {
        this.routeFunction = routeFunction;
        this.handler = handler;
        this.type = type;
    }

    public RouteBuildItem(Function<Router, Route> routeFunction, Handler<RoutingContext> handler) {
        this(routeFunction, handler, HandlerType.NORMAL);
    }

    public Handler<RoutingContext> getHandler() {
        return handler;
    }

    public HandlerType getType() {
        return type;
    }

    public Function<Router, Route> getRouteFunction() {
        return routeFunction;
    }

}
