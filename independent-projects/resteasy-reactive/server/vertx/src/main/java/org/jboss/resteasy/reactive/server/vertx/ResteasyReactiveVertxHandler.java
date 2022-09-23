package org.jboss.resteasy.reactive.server.vertx;

import org.jboss.resteasy.reactive.server.handlers.RestInitialHandler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class ResteasyReactiveVertxHandler implements Handler<RoutingContext> {

    private final RestInitialHandler handler;

    public ResteasyReactiveVertxHandler(RestInitialHandler handler) {
        this.handler = handler;
    }

    @Override
    public void handle(RoutingContext event) {
        handler.beginProcessing(event);
    }
}
