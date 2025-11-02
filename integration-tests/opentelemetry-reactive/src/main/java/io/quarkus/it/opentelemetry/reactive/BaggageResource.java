package io.quarkus.it.opentelemetry.reactive;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.WithSpan;

@ApplicationScoped
@Path("baggage")
public class BaggageResource {

    @Path("build")
    @WithSpan
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() {
        try (Scope ignored = Baggage.current().toBuilder().put("key", "baggage-value").build().makeCurrent()) {
            String value = Baggage.current().getEntryValue("key");
            if (!"baggage-value".equals(value)) {
                throw new RuntimeException("Baggage is missing the first value!");
            }
            return value;
        }
    }
}
