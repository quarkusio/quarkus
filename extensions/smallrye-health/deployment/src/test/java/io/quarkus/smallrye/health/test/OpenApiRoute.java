package io.quarkus.smallrye.health.test;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.vertx.core.http.HttpMethod;

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
