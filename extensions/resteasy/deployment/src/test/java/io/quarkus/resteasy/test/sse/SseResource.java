package io.quarkus.resteasy.test.sse;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

@Path("sse")
public class SseResource {

    @Context
    private Sse sse;

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void subscribe(@Context SseEventSink sink) throws IOException {
        if (sink == null) {
            throw new IllegalStateException("No client connected.");
        }
        SseBroadcaster sseBroadcaster = sse.newBroadcaster();

        sseBroadcaster.register(sink);
        sseBroadcaster.broadcast(sse.newEventBuilder().data("hello").build());
    }
}
