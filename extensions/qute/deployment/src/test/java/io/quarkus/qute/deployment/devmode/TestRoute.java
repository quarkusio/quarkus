package io.quarkus.qute.deployment.devmode;

import jakarta.inject.Inject;

import io.quarkus.qute.Template;
import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;

public class TestRoute {

    @Inject
    Template test;

    @Route(path = "test")
    public void test(RoutingContext ctx) {
        ctx.end(test.render());
    }

}
