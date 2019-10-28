package io.quarkus.vertx.http.runtime;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

class ResumeHandler implements Handler<RoutingContext> {

    final Handler<RoutingContext> next;

    ResumeHandler(Handler<RoutingContext> next) {
        this.next = next;
    }

    @Override
    public void handle(RoutingContext event) {
        //we resume the request to make up for the pause that was done in the root handler
        //this maintains normal vert.x semantics in the handlers
        event.request().resume();
        next.handle(event);
    }
}
