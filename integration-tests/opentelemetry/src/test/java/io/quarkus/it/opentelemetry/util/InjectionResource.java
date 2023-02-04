package io.quarkus.it.opentelemetry.util;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.smallrye.mutiny.Uni;

@Path("/otel/injection")
@RequestScoped
public class InjectionResource {

    @Inject
    OpenTelemetry openTelemetry;

    @Inject
    Tracer tracer;

    @Inject
    Span span;

    @Inject
    Baggage baggage;

    @GET
    public Response verifyOTelInjections() {
        verifyInjections();
        return Response.ok().build();
    }

    @GET
    @Path("/async")
    public Uni<Response> verifyOTelInjectionsAsync() {
        verifyInjections();
        return Uni.createFrom().item(Response.ok().build());
    }

    private void verifyInjections() {
        Assertions.assertNotNull(openTelemetry, "OpenTelemetry cannot be injected");
        Assertions.assertNotNull(tracer, "Tracer cannot be injected");
        Assertions.assertNotNull(span, "Span cannot be injected");
        Assertions.assertNotNull(openTelemetry, "Baggage cannot be injected");

        Assertions.assertEquals(GlobalOpenTelemetry.get(), openTelemetry);
        Assertions.assertEquals(Span.current().getSpanContext(), span.getSpanContext());
        Assertions.assertEquals(Baggage.current().size(), baggage.size());
        baggage.asMap().forEach((s, baggageEntry) -> Assertions.assertEquals(baggageEntry, baggage.asMap().get(s)));
    }
}
