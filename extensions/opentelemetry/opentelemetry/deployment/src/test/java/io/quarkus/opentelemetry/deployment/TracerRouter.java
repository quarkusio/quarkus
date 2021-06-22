package io.quarkus.opentelemetry.deployment;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

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
    }
}
