package io.quarkus.qute.deployment.devmode;

import java.net.URI;

import jakarta.inject.Inject;

import io.quarkus.qute.Template;
import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;

public class SourceRoute {

    @Inject
    Template test;

    @Route(path = "test")
    public void test(RoutingContext ctx) {
        URI source = test.getSource().orElse(null);
        if (source == null) {
            ctx.response().setStatusCode(500).end();
        } else {
            ctx.end(source.toString());
        }
    }

}
