package io.quarkus.it.opentelemetry.vertx;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import io.smallrye.mutiny.Uni;
import io.vertx.core.eventbus.DeliveryOptions;
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
    @Path("/person2")
    @Produces("text/plain")
    public Uni<String> helloToPersonWithHeaders(JsonObject json) {
        return bus.<String> request(
                "person-headers",
                new Person(json.getString("firstName"), json.getString("lastName")),
                new DeliveryOptions().addHeader("header", "headerValue"))
                .onItem().transform(Message::body);
    }

    @POST
    @Path("/pet")
    public Uni<String> helloToPet(JsonObject json) {
        return bus.<String> request("pets", new Pet(json.getString("name"), json.getString("kind")))
                .onItem().transform(Message::body);
    }

}
