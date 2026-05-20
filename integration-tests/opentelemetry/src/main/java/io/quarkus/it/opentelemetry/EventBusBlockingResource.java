package io.quarkus.it.opentelemetry;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.eventbus.EventBus;

@Path("/eventbus-blocking")
public class EventBusBlockingResource {

    @Inject
    EventBus eventBus;

    @GET
    @Path("/publish")
    public String publish() {
        eventBus.publish("blocking-publish", "test");
        return "ok";
    }

    @GET
    @Path("/publish-annotation")
    public String publishAnnotation() {
        eventBus.publish("blocking-annotation-publish", "test");
        return "ok";
    }

    @Singleton
    public static class EventConsumers {

        @Inject
        Tracer tracer;

        @ConsumeEvent(value = "blocking-publish", blocking = true)
        void consumeBlocking(String message) {
            Span span = tracer.spanBuilder("inside-blocking-handler").startSpan();
            try (Scope scope = span.makeCurrent()) {
                span.setAttribute("test.message", message);
            } finally {
                span.end();
            }
        }

        @ConsumeEvent("blocking-annotation-publish")
        @Blocking
        void consumeBlockingAnnotation(String message) {
            Span span = tracer.spanBuilder("inside-blocking-annotation-handler").startSpan();
            try (Scope scope = span.makeCurrent()) {
                span.setAttribute("test.message", message);
            } finally {
                span.end();
            }
        }
    }
}
