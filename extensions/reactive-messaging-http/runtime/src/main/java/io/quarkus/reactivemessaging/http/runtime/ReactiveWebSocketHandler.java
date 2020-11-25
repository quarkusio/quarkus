package io.quarkus.reactivemessaging.http.runtime;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

class ReactiveWebSocketHandler implements Handler<RoutingContext> {
    private final ReactiveWebSocketHandlerBean handler;

    ReactiveWebSocketHandler(ReactiveWebSocketHandlerBean handler) {
        this.handler = handler;
    }

    @Override
    public void handle(RoutingContext event) {
        handler.handle(event);
    }
}
