package io.quarkus.qute.deployment.devmode;

import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class NoRestartRoute {

    private String id;

    @Location("foo/norestart")
    Template norestart;

    @Inject
    Template bar;

    @Route(path = "norestart")
    public void test(RoutingContext ctx) {
        ctx.end(norestart.data("id", id).render());
    }

    @Route(path = "bar")
    public void testBar(RoutingContext ctx) {
        ctx.end(bar.data("id", id).render());
    }

    @PostConstruct
    void init() {
        id = UUID.randomUUID().toString();
    }

}
