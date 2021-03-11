package io.quarkus.micrometer.test;

import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.Route.HttpMethod;
import io.quarkus.vertx.web.RouteBase;

@RouteBase(path = "/vertx")
public class VertxWebEndpoint {
    @Route(path = "item/:id", methods = HttpMethod.GET)
    public String item(@Param("id") Integer id) {
        return "message with id " + id;
    }

    @Route(path = "item/:id/:sub", methods = HttpMethod.GET)
    public String item(@Param("id") Integer id, @Param("sub") Integer sub) {
        return "message with id " + id + " and sub " + sub;
    }
}
