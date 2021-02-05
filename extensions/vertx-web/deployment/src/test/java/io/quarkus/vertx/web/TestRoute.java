package io.quarkus.vertx.web;

import static io.quarkus.vertx.web.Route.HttpMethod.GET;
import static io.quarkus.vertx.web.Route.HttpMethod.OPTIONS;
import static io.quarkus.vertx.web.Route.HttpMethod.POST;

import io.vertx.ext.web.RoutingContext;

public class TestRoute {

    @Route(path = "/test", methods = { GET, OPTIONS, POST })
    void getRoutes(RoutingContext context) {
        context.response().setStatusCode(200).end("test route");
    }
}
