package io.quarkus.it.vertx;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;
import io.vertx.mutiny.core.eventbus.MessageConsumer;

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
    public Uni<String> eb() {
        String address = UUID.randomUUID().toString();

        // Use the event bus bean.
        MessageConsumer<String> consumer = eventBus.consumer(address);
        consumer.handler(m -> {
            m.reply("hello " + m.body());
            consumer.unregister();
        });

        // Use the Vert.x bean.
        return vertx.eventBus().<String> request(address, "quarkus")
                .onItem().transform(Message::body);
    }

}
