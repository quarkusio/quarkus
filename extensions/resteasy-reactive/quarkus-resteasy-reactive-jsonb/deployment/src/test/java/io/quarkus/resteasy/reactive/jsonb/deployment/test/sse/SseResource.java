package io.quarkus.resteasy.reactive.jsonb.deployment.test.sse;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

import org.jboss.resteasy.reactive.RestSseElementType;

import io.smallrye.mutiny.Multi;

@Path("sse")
public class SseResource {

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void sse(Sse sse, SseEventSink sink) throws IOException {
        if (sink == null) {
            throw new IllegalStateException("No client connected.");
        }
        SseBroadcaster sseBroadcaster = sse.newBroadcaster();

        sseBroadcaster.register(sink);
        sseBroadcaster.broadcast(sse.newEventBuilder().data("hello").build())
                .thenCompose(v -> sseBroadcaster.broadcast(sse.newEventBuilder().data("stef").build()))
                .thenAccept(v -> sseBroadcaster.close());
    }

    @Path("multi")
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<String> multiText() {
        return Multi.createFrom().items("hello", "stef");
    }

    @Path("json")
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestSseElementType(MediaType.APPLICATION_JSON)
    public void sseJson(Sse sse, SseEventSink sink) throws IOException {
        if (sink == null) {
            throw new IllegalStateException("No client connected.");
        }
        SseBroadcaster sseBroadcaster = sse.newBroadcaster();

        sseBroadcaster.register(sink);
        sseBroadcaster.broadcast(sse.newEventBuilder().data(new Message("hello")).build())
                .thenCompose(v -> sseBroadcaster.broadcast(sse.newEventBuilder().data(new Message("stef")).build()))
                .thenAccept(v -> sseBroadcaster.close());
    }

    @Path("json2")
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void sseJson2(Sse sse, SseEventSink sink) throws IOException {
        if (sink == null) {
            throw new IllegalStateException("No client connected.");
        }
        SseBroadcaster sseBroadcaster = sse.newBroadcaster();

        // Same as sseJson but set mediaType in builder
        sseBroadcaster.register(sink);
        sseBroadcaster
                .broadcast(sse.newEventBuilder().data(new Message("hello")).mediaType(MediaType.APPLICATION_JSON_TYPE).build())
                .thenCompose(v -> sseBroadcaster.broadcast(
                        sse.newEventBuilder().mediaType(MediaType.APPLICATION_JSON_TYPE).data(new Message("stef")).build()))
                .thenAccept(v -> sseBroadcaster.close());
    }

    @Path("json/multi")
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestSseElementType(MediaType.APPLICATION_JSON)
    public Multi<Message> multiJson() {
        return Multi.createFrom().items(new Message("hello"), new Message("stef"));
    }

}
