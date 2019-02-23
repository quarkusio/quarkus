package io.quarkus.vertx.tests;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;

@Path("/")
public class VertxProducerResource {

    @Inject
    Vertx vertx;

    @Inject
    EventBus eventBus;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Map<String, Boolean> test() {
        Map<String, Boolean> map = new HashMap<>();
        map.put("vertx", vertx != null);
        map.put("eventbus", eventBus != null);
        return map;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/eventBus")
    public CompletionStage<String> eb() {
        String address = UUID.randomUUID().toString();

        // Use the event bus bean.
        MessageConsumer<String> consumer = eventBus.consumer(address);
        consumer.handler(m -> {
            m.reply("hello " + m.body());
            consumer.unregister();
        });

        CompletableFuture<String> future = new CompletableFuture<>();
        // Use the Vert.x bean.
        vertx.eventBus().<String> send(address, "quarkus", ar -> {
            if (ar.failed()) {
                future.completeExceptionally(ar.cause());
            } else {
                future.complete(ar.result().body());
            }
        });

        return future;
    }

}
