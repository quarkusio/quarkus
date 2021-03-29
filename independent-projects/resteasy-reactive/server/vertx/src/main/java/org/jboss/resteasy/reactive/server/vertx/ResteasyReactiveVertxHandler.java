package org.jboss.resteasy.reactive.server.vertx;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.jboss.resteasy.reactive.server.handlers.RestInitialHandler;

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
