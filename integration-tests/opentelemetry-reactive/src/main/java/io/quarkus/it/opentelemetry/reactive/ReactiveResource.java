package io.quarkus.it.opentelemetry.reactive;

import java.time.Duration;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.smallrye.mutiny.Uni;

@Path("/reactive")
public class ReactiveResource {
    @Inject
    Tracer tracer;
    @Inject
    @RestClient
    ReactiveRestClient client;

    @GET
    public Uni<String> helloGet(@QueryParam("name") String name) {
        Span span = tracer.spanBuilder("helloGet").startSpan();
        return Uni.createFrom().item("Hello " + name).onItem().delayIt().by(Duration.ofSeconds(2))
                .eventually((Runnable) span::end);
    }

    @GET
    @Path("/multiple")
    public Uni<String> helloMultiple() {
        return Uni.combine().all().unis(client.helloGet("Naruto"), client.helloGet("Goku"))
                .combinedWith((s, s2) -> s + " and " + s2);
    }

    @POST
    public Uni<String> helloPost(String body) {
        Span span = tracer.spanBuilder("helloPost").startSpan();
        return Uni.createFrom().item("Hello " + body).onItem().delayIt().by(Duration.ofSeconds(2))
                .eventually((Runnable) span::end);
    }
}
