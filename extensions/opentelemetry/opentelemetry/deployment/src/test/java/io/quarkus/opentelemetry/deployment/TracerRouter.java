package io.quarkus.opentelemetry.deployment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.opentelemetry.api.trace.Tracer;
import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.web.Router;

@ApplicationScoped
public class TracerRouter {
    @Inject
    Router router;
    @Inject
    Tracer tracer;

    public void register(@Observes StartupEvent ev) {
        router.get("/tracer").handler(rc -> {
            tracer.spanBuilder("io.quarkus.vertx.opentelemetry").startSpan()
                    .setAttribute("test.message", "hello!")
                    .end();
            rc.response().end("Hello Tracer!");
        });

        router.get("/hello/:name").handler(rc -> {
            String name = rc.pathParam("name");
            if (name.equals("Naruto")) {
                rc.response().end("hello " + name);
            } else {
                rc.response().setStatusCode(404).end();
            }
        });
    }
}
