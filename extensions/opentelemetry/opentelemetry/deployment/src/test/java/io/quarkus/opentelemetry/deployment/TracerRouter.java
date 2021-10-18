package io.quarkus.opentelemetry.deployment;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.web.Router;

@ApplicationScoped
public class TracerRouter {
    @Inject
    Router router;
    @Inject
    Tracer tracer;
    @Inject
    TestMdcCapturer testMdcCapturer;

    public void register(@Observes StartupEvent ev) {
        router.get("/tracer").handler(rc -> {

            testMdcCapturer.captureMdc();

            Span span = tracer.spanBuilder("io.quarkus.vertx.opentelemetry").startSpan()
                    .setAttribute("test.message", "hello!");
            try (Scope ignored = span.makeCurrent()) {
                testMdcCapturer.captureMdc();
            } finally {
                span.end();
            }
            rc.response().end("Hello Tracer!");
        });
    }
}
