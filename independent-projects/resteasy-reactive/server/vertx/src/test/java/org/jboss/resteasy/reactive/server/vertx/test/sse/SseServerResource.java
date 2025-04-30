package org.jboss.resteasy.reactive.server.vertx.test.sse;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

import org.jboss.logging.Logger;

@Path("sse")
public class SseServerResource {
    private static SseBroadcaster sseBroadcaster;

    private static OutboundSseEvent.Builder eventBuilder;
    private static CountDownLatch closeLatch;
    private static CountDownLatch errorLatch;

    private static final Logger logger = Logger.getLogger(SseServerResource.class);

    @Inject
    public SseServerResource(@Context Sse sse) {
        logger.info("Initialized SseServerResource " + this.hashCode());
        if (Objects.isNull(eventBuilder)) {
            eventBuilder = sse.newEventBuilder();
        }
        if (Objects.isNull(sseBroadcaster)) {
            sseBroadcaster = sse.newBroadcaster();
            logger.info("Initializing broadcaster " + sseBroadcaster.hashCode());
            sseBroadcaster.onClose(sseEventSink -> {
                CountDownLatch latch = SseServerResource.getCloseLatch();
                logger.info(String.format("Called on close, counting down latch %s", latch.hashCode()));
                latch.countDown();
            });
            sseBroadcaster.onError((sseEventSink, throwable) -> {
                CountDownLatch latch = SseServerResource.getErrorLatch();
                logger.info(String.format("There was an error, counting down latch %s", latch.hashCode()));
                latch.countDown();
            });
        }
    }

    @GET
    @Path("subscribe")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void subscribe(@Context SseEventSink sseEventSink) {
        logger.info(this.hashCode() + " /subscribe");
        setLatches();
        getSseBroadcaster().register(sseEventSink);
        sseEventSink.send(eventBuilder.data(sseEventSink.hashCode()).build());
    }

    @POST
    @Path("broadcast")
    public Response broadcast() {
        logger.info(this.hashCode() + " /broadcast");
        getSseBroadcaster().broadcast(eventBuilder.data(Instant.now()).build());
        return Response.ok().build();
    }

    @GET
    @Path("onclose-callback")
    public Response callback() throws InterruptedException {
        logger.info(this.hashCode() + " /onclose-callback, waiting for latch " + closeLatch.hashCode());
        boolean onCloseWasCalled = closeLatch.await(10, TimeUnit.SECONDS);
        return Response.ok(onCloseWasCalled).build();
    }

    @GET
    @Path("onerror-callback")
    public Response errorCallback() throws InterruptedException {
        logger.info(this.hashCode() + " /onerror-callback, waiting for latch " + errorLatch.hashCode());
        boolean onErrorWasCalled = errorLatch.await(2, TimeUnit.SECONDS);
        return Response.ok(onErrorWasCalled).build();
    }

    private static SseBroadcaster getSseBroadcaster() {
        logger.info("using broadcaster " + sseBroadcaster.hashCode());
        return sseBroadcaster;
    }

    public static void setLatches() {
        closeLatch = new CountDownLatch(1);
        errorLatch = new CountDownLatch(1);
        logger.info(String.format("Setting latches: \n  closeLatch:  %s\n  errorLatch: %s",
                closeLatch.hashCode(), errorLatch.hashCode()));
    }

    public static CountDownLatch getCloseLatch() {
        return closeLatch;
    }

    public static CountDownLatch getErrorLatch() {
        return errorLatch;
    }
}
