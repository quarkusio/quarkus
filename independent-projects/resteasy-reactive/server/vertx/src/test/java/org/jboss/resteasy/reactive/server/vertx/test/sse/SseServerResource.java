package org.jboss.resteasy.reactive.server.vertx.test.sse;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;

@Path("sse")
@ApplicationScoped
public class SseServerResource {

    public static final Logger logger = Logger.getLogger(SseServerResource.class.getName());
    private static SseBroadcaster sseBroadcaster;
    private static OutboundSseEvent.Builder eventBuilder;

    private static CountDownLatch closeLatch;
    private static CountDownLatch errorLatch;

    @Inject
    public SseServerResource(@Context Sse sse) {
        if (Objects.isNull(eventBuilder)) {
            eventBuilder = sse.newEventBuilder();
        }
        if (Objects.isNull(sseBroadcaster)) {
            sseBroadcaster = sse.newBroadcaster();
            sseBroadcaster.onClose(this::onClose);
            sseBroadcaster.onError(this::onError);
        }
    }

    private synchronized void onError(SseEventSink sseEventSink, Throwable throwable) {
        logger.severe(String.format("There was an error for sseEventSink %s: %s",
                sseEventSink.hashCode(), throwable.getMessage()));
        errorLatch.countDown();
    }

    private synchronized void onClose(SseEventSink sseEventSink) {
        logger.info(String.format("Called on close for %s", sseEventSink.hashCode()));
        closeLatch.countDown();
    }

    @GET
    @Path("subscribe")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void subscribe(@Context SseEventSink sseEventSink) {
        sseBroadcaster.register(sseEventSink);
        closeLatch = new CountDownLatch(1);
        errorLatch = new CountDownLatch(1);
        sseEventSink.send(eventBuilder.data(sseEventSink.hashCode()).build());
    }

    @POST
    @Path("broadcast")
    public Response broadcast() {
        sseBroadcaster.broadcast(eventBuilder.data(Instant.now()).build());
        return Response.ok().build();
    }

    @GET
    @Path("onclose-callback")
    public Response callback() throws InterruptedException {
        boolean onCloseWasCalled = awaitClosedCallback();
        return Response.ok(onCloseWasCalled).build();
    }

    @GET
    @Path("onerror-callback")
    public Response errorCallback() throws InterruptedException {
        boolean onErrorWasCalled = awaitErrorCallback();
        return Response.ok(onErrorWasCalled).build();
    }

    private synchronized boolean awaitClosedCallback() throws InterruptedException {
        return closeLatch.await(10, TimeUnit.SECONDS);
    }

    private synchronized boolean awaitErrorCallback() throws InterruptedException {
        return errorLatch.await(2, TimeUnit.SECONDS);
    }
}
