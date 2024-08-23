package io.quarkus.smallrye.openapi.test.vertx;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.Route.HttpMethod;
import io.quarkus.vertx.web.RouteBase;

@ApplicationScoped
@RouteBase(path = "resource", consumes = "application/json", produces = "application/json")
public class OpenApiRoute {

    @Route(path = "/", methods = HttpMethod.GET)
    public String root() {
        return "resource";
    }

    @Route(path = "/test-enums", methods = HttpMethod.GET)
    public Query testEnums(@Param("query") String query) {
        return Query.QUERY_PARAM_1;
    }

    public enum Query {
        QUERY_PARAM_1,
        QUERY_PARAM_2;
    }
}
