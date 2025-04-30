package io.quarkus.it.amazon.lambda.rest.reactive.routes;

import static io.quarkus.vertx.web.Route.HttpMethod.GET;
import static io.quarkus.vertx.web.Route.HttpMethod.POST;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RoutingExchange;
import io.vertx.ext.web.RoutingContext;

public class GreetingVertx {
    @Route(path = "/vertx/hello", methods = GET)
    void hello(RoutingContext context) {
        context.response().headers().set("Content-Type", "text/plain");
        context.response().setStatusCode(200).end("hello");
    }

    @Route(path = "/vertx/hello", methods = POST)
    void helloPost(RoutingContext context) {
        String name = context.getBodyAsString();
        context.response().headers().set("Content-Type", "text/plain");
        context.response().setStatusCode(200).end("hello " + name);
    }

    @Route(path = "/vertx/rx/hello", methods = GET)
    void rxHelloGet(RoutingContext context) {
        context.response().headers().set("Content-Type", "text/plain");
        context.response().setStatusCode(200).end("hello");
    }

    @Route(path = "/vertx/rx/hello", methods = POST)
    void rxHelloPost(RoutingContext context) {
        String name = context.getBodyAsString();
        context.response().headers().set("Content-Type", "text/plain");
        context.response().setStatusCode(200).end("hello " + name);
    }

    @Route(path = "/vertx/exchange/hello", methods = GET)
    void exchange(RoutingExchange exchange) {
        exchange.response().headers().set("Content-Type", "text/plain");
        exchange.ok("hello");
    }
}
