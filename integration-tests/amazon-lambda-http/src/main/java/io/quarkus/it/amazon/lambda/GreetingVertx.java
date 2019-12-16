package io.quarkus.it.amazon.lambda;

import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import java.util.HashMap;

import javax.ws.rs.GET;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RoutingExchange;
import io.vertx.ext.web.RoutingContext;

public class GreetingVertx {
    @Route(path = "/vertx/hello", methods = GET)
    void hello(RoutingContext context) {
        context.response().headers().set("Content-Type", "text/plain");
        context.response().setStatusCode(200).end("hello");
    }

    @Route(path = "/vertx/json", methods = GET)
    void json(RoutingContext context) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        HashMap<Object, Object> map = new HashMap<>();
        map.put("hello", "world");
        String json = mapper.writeValueAsString(map);
        context.response().headers().set("Content-Type", MediaType.APPLICATION_JSON);
        context.response().setStatusCode(200).end(json);
    }

    @Route(path = "/vertx/hello", methods = POST)
    void helloPost(RoutingContext context) {
        String name = context.getBodyAsString();
        context.response().headers().set("Content-Type", "text/plain");
        context.response().setStatusCode(200).end("hello " + name);
    }

    @Route(path = "/vertx/rx/hello", methods = GET)
    void rxHelloGet(io.vertx.reactivex.ext.web.RoutingContext context) {
        context.response().headers().set("Content-Type", "text/plain");
        context.response().setStatusCode(200).end("hello");
    }

    @Route(path = "/vertx/rx/hello", methods = POST)
    void rxHelloPost(io.vertx.reactivex.ext.web.RoutingContext context) {
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
