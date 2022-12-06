package io.quarkus.it.vertx;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;

@Path("/event-bus")
public class EventBusSender {

    @Inject
    EventBus bus;

    @POST
    @Path("/person")
    public Uni<String> helloToPerson(JsonObject json) {
        return bus.<String> request("persons", json.getString("name"))
                .onItem().transform(Message::body);
    }

    @POST
    @Path("/pet")
    public Uni<String> helloToPet(JsonObject json) {
        return bus.<String> request("pets", new Pet(json.getString("name"), json.getString("kind")))
                .onItem().transform(Message::body);
    }

}
