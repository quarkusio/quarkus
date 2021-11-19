package io.quarkus.it.opentelemetry.vertx;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.web.Router;

@ApplicationScoped
public class HelloRouter {
    @Inject
    Router router;

    public void register(@Observes StartupEvent ev) {
        router.get("/hello").handler(rc -> rc.response().end("hello"));
        router.get("/hello/:name").handler(rc -> rc.response().end("hello " + rc.pathParam("name")));
    }
}
