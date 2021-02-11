package io.quarkus.vertx.web;

import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.OPTIONS;
import static io.vertx.core.http.HttpMethod.POST;

import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.ext.web.RoutingContext;

public class DevModeRoute {

    @Route(path = "/test", methods = { GET, OPTIONS, POST })
    void getRoutes(RoutingContext context) {
        context.response().setStatusCode(200).end("test route");
    }

    @Route(path = "/assert", methods = GET)
    void assertVertx(RoutingContext context) {
        if (VertxCoreRecorder.getVertx().get() == context.vertx()) {
            context.response().end("OK");
        } else {
            context.fail(new RuntimeException("Incorrect vertx in use"));
        }
    }
}
