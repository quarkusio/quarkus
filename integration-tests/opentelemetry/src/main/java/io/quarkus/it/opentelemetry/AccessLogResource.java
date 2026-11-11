package io.quarkus.it.opentelemetry;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.MDC;

import io.smallrye.mutiny.Uni;

@Path("/access-log-test")
public class AccessLogResource {

    @GET
    @Path("/async")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> asyncEndpoint() {
        String body = String.format("{\"traceId\": \"%s\", \"spanId\": \"%s\"}", MDC.get("traceId"), MDC.get("spanId"));
        return Uni.createFrom().item(Response.ok(body).build());
    }
}
