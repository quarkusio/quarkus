package org.acme.reactive.routes;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.vertx.web.Route;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class MyDeclarativeRoutes {

    @Route(path = "/", methods = HttpMethod.GET)
    public void handle(RoutingContext rc) {
        rc.response().end("hello");
    }

    @Route(path = "/hello", methods = HttpMethod.GET)
    public void greetings(RoutingContext rc) {
        String name = rc.request().getParam("name");
        if (name == null) {
            name = "world";
        }
        rc.response().end("hello " + name);
    }
}
