package io.quarkus.resteasy.reactive.jackson.deployment.test.streams;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;

import org.jboss.resteasy.reactive.RestMulti;
import org.jboss.resteasy.reactive.RestStreamElementType;
import org.jboss.resteasy.reactive.common.util.RestMediaType;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Path("streams")
public class StreamResource {

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void sse(Sse sse, SseEventSink sink) {
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
    @RestStreamElementType(MediaType.APPLICATION_JSON)
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

    @Blocking
    @Path("blocking/json")
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public void blockingSseJson(Sse sse, SseEventSink sink) throws IOException {
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
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<Message> multiJson() {
        return RestMulti.fromMultiData(Multi.createFrom().items(new Message("hello"), new Message("stef")))
                .header("foo", "bar").build();
    }

    @Path("json/multi2")
    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<Message> multiDefaultElementType() {
        return Multi.createFrom().items(new Message("hello"), new Message("stef"));
    }

    @Path("ndjson/multi")
    @GET
    @Produces(RestMediaType.APPLICATION_NDJSON)
    public Multi<Message> multiNdJson() {
        return Multi.createFrom().items(new Message("hello"), new Message("stef"));
    }

    @Path("ndjson/multi2")
    @GET
    @Produces(RestMediaType.APPLICATION_NDJSON)
    public Multi<Message> multiNdJson2() {

        return RestMulti.fromUniResponse(
                Uni.createFrom().item(
                        () -> new Wrapper(Multi.createFrom().items(new Message("hello"), new Message("stef")),
                                new AbstractMap.SimpleEntry<>("foo", "bar"), 222)),
                Wrapper::getData,
                Wrapper::getHeaders,
                Wrapper::getStatus);
    }

    @Path("stream-json/multi")
    @GET
    @Produces(RestMediaType.APPLICATION_STREAM_JSON)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<Message> multiStreamJson() {
        return Multi.createFrom().items(new Message("hello"), new Message("stef"));
    }

    /**
     * Reproduce <a href="https://github.com/quarkusio/quarkus/issues/30044">#30044</a>.
     */
    @Path("stream-json/multi/fast")
    @GET
    @Produces(RestMediaType.APPLICATION_STREAM_JSON)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<Message> multiStreamJsonFast() {
        List<UUID> ids = new ArrayList<>(5000);
        for (int i = 0; i < 5000; i++) {
            ids.add(UUID.randomUUID());
        }
        return RestMulti.fromMultiData(Multi.createFrom().items(ids::stream)
                .onItem().transform(id -> new Message(id.toString()))
                .onOverflow().buffer(81920)).header("foo", "bar").build();
    }

    private static final class Wrapper {
        public final Multi<Message> data;

        public final Map<String, List<String>> headers;
        private final Integer status;

        public Wrapper(Multi<Message> data, Map.Entry<String, String> header, Integer status) {
            this.data = data;
            this.status = status;
            this.headers = Map.of(header.getKey(), List.of(header.getValue()));
        }

        public Multi<Message> getData() {
            return data;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public Integer getStatus() {
            return status;
        }
    }

}
