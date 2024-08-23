package io.quarkus.virtual.vertx;

import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.mutiny.core.eventbus.EventBus;

@RunOnVirtualThread
@Path("/")
public class VertxEventBusResource {

    @Inject
    EventBus bus;

    @GET
    @Path("/one-way")
    public void sentOneWay() {
        bus.send("one-way", "hello");
    }

    @GET
    @Path("/one-way-verify")
    public List<String> oneWayVerification() {
        return EventBusConsumer.ONE_WAY;
    }

    @GET
    @Path("/request-reply")
    public String requestReply() {
        return bus.<String> request("request-reply", "hello").map(m -> m.body()).await().atMost(Duration.ofSeconds(10));
    }

}
