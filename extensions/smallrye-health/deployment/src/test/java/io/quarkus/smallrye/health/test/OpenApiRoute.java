package io.quarkus.smallrye.health.test;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.Route.HttpMethod;
import io.quarkus.vertx.web.RouteBase;

/**
 * A dummy test REST endpoint.
 */
@ApplicationScoped
@RouteBase(path = "resource", consumes = "application/json", produces = "application/json")
public class OpenApiRoute {

    @Route(path = "/", methods = HttpMethod.GET)
    public String root() {
        return "resource";
    }
}
