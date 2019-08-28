package io.quarkus.it.vertx;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import io.vertx.axle.core.eventbus.EventBus;
import io.vertx.axle.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

@Path("/event-bus")
public class EventBusSender {

    @Inject
    EventBus bus;

    @POST
    @Path("/person")
    public CompletionStage<String> helloToPerson(JsonObject json) {
        return bus.<String> request("persons", json.getString("name")).thenApply(Message::body);
    }

    @POST
    @Path("/pet")
    public CompletionStage<String> helloToPet(JsonObject json) {
        return bus.<String> request("pets", new Pet(json.getString("name"), json.getString("kind")))
                .thenApply(Message::body);
    }

}
