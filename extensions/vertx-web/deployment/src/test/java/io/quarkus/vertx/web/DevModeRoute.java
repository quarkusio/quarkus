package io.quarkus.vertx.web;

import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.ext.web.RoutingContext;

public class DevModeRoute {

    @Route(path = "/test", methods = { Route.HttpMethod.GET, Route.HttpMethod.OPTIONS, Route.HttpMethod.POST })
    void getRoutes(RoutingContext context) {
        context.response().setStatusCode(200).end("test route");
    }

    @Route(path = "/assert", methods = Route.HttpMethod.GET)
    void assertVertx(RoutingContext context) {
        if (VertxCoreRecorder.getVertx().get() == context.vertx()) {
            context.response().end("OK");
        } else {
            context.fail(new RuntimeException("Incorrect vertx in use"));
        }
    }
}
