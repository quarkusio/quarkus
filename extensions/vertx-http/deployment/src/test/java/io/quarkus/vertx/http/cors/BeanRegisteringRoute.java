package io.quarkus.vertx.http.cors;

import jakarta.enterprise.event.Observes;

import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class BeanRegisteringRoute {

    public void init(@Observes Router router) {
        Handler<RoutingContext> handler = rc -> rc.response().end("test route");

        router.get("/test").handler(handler);
        router.options("/test").handler(handler);
    }
}
