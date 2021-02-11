package io.quarkus.vertx.http.runtime;

import java.util.function.Consumer;

import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

public class HandlerConsumer implements Consumer<Route> {
    Handler<RoutingContext> handler;

    public Handler<RoutingContext> getHandler() {
        return handler;
    }

    public void setHandler(Handler<RoutingContext> handler) {
        this.handler = handler;
    }

    @Override
    public void accept(Route route) {
        route.handler(handler);
    }
}
