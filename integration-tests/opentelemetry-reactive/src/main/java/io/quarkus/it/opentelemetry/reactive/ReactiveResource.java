package io.quarkus.it.opentelemetry.reactive;

import java.time.Duration;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.smallrye.mutiny.Uni;

@Path("/reactive")
public class ReactiveResource {
    @Inject
    Tracer tracer;

    @GET
    public Uni<String> helloGet(@QueryParam("name") String name) {
        Span span = tracer.spanBuilder("helloGet").startSpan();
        return Uni.createFrom().item("Hello " + name).onItem().delayIt().by(Duration.ofSeconds(2))
                .eventually((Runnable) span::end);
    }

    @POST
    public Uni<String> helloPost(String body) {
        Span span = tracer.spanBuilder("helloPost").startSpan();
        return Uni.createFrom().item("Hello " + body).onItem().delayIt().by(Duration.ofSeconds(2))
                .eventually((Runnable) span::end);
    }
}
