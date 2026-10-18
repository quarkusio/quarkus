package org.jboss.resteasy.reactive.server.vertx;

import java.util.function.Consumer;

import org.jboss.resteasy.reactive.server.handlers.RestInitialHandler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class ResteasyReactiveVertxHandler implements Handler<RoutingContext> {

    private final RestInitialHandler handler;
    private final Consumer<RoutingContext> eventCustomizer;

    public ResteasyReactiveVertxHandler(Consumer<RoutingContext> eventCustomizer, RestInitialHandler handler) {
        this.handler = handler;
        this.eventCustomizer = eventCustomizer;
    }

    @Override
    public void handle(RoutingContext event) {
        eventCustomizer.accept(event);
        handler.beginProcessing(event);
    }
}
