package io.quarkus.it.amazon.lambda.v1;

import static io.quarkus.vertx.web.Route.HttpMethod.GET;

import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;

public class SecurityCheckVertx {
    @Route(path = "/vertx/security", methods = GET)
    void hello(RoutingContext context) {
        context.response().headers().set("Content-Type", "text/plain");
        context.response().setStatusCode(200)
                .end(((QuarkusHttpUser) context.user()).getSecurityIdentity().getPrincipal().getName());
    }
}
