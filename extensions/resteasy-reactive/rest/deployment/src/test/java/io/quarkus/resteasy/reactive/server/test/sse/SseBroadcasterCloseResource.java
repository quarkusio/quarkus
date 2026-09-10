package io.quarkus.resteasy.reactive.server.test.sse;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;

@Path("sse-broadcaster")
public class SseBroadcasterCloseResource {

    private static volatile SseBroadcaster broadcaster;
    private static final AtomicInteger onCloseCount = new AtomicInteger();

    @GET
    @Path("register")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void register(SseEventSink eventSink, Sse sse) {
        // Register the sink with the shared broadcaster and keep it open; it is closed when the
        // broadcaster is closed. Send one event so the client knows it is connected.
        broadcaster(sse).register(eventSink);
        eventSink.send(sse.newEvent("connected"));
    }

    @GET
    @Path("close")
    public String close() {
        SseBroadcaster current = broadcaster;
        if (current != null) {
            current.close();
        }
        return "OK";
    }

    @GET
    @Path("close-count")
    public String closeCount() {
        return String.valueOf(onCloseCount.get());
    }

    private static synchronized SseBroadcaster broadcaster(Sse sse) {
        if (broadcaster == null) {
            SseBroadcaster created = sse.newBroadcaster();
            created.onClose(sink -> onCloseCount.incrementAndGet());
            broadcaster = created;
        }
        return broadcaster;
    }
}
