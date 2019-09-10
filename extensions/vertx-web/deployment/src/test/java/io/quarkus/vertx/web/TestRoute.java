package io.quarkus.vertx.web;

import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.OPTIONS;
import static io.vertx.core.http.HttpMethod.POST;

import io.vertx.ext.web.RoutingContext;

public class TestRoute {

    @Route(path = "/test", methods = { GET, OPTIONS, POST })
    void getRoutes(RoutingContext context) {
        context.response().setStatusCode(200).end("test route");
    }
}
