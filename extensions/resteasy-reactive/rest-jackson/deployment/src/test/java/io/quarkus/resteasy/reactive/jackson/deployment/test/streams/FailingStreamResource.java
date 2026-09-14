package io.quarkus.resteasy.reactive.jackson.deployment.test.streams;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestStreamElementType;
import org.jboss.resteasy.reactive.common.util.RestMediaType;

import io.smallrye.mutiny.Multi;

@Path("failing-streams")
public class FailingStreamResource {

    @GET
    @Path("json/before-first-item")
    @Produces(MediaType.APPLICATION_JSON)
    public Multi<Message> jsonFailingBeforeFirstItem() {
        return Multi.createFrom().failure(new IllegalStateException("boom"));
    }

    @GET
    @Path("json/after-items")
    @Produces(MediaType.APPLICATION_JSON)
    public Multi<Message> jsonFailingAfterItems() {
        return Multi.createFrom().items(new Message("a"), new Message("b"))
                .onCompletion().failWith(() -> new IllegalStateException("boom"));
    }

    @GET
    @Path("ndjson/after-items")
    @Produces(RestMediaType.APPLICATION_NDJSON)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<Message> ndjsonFailingAfterItems() {
        return Multi.createFrom().items(new Message("a"), new Message("b"))
                .onCompletion().failWith(() -> new IllegalStateException("boom"));
    }
}
