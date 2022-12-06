package io.quarkus.it.kafka;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.smallrye.mutiny.Uni;

@Path("/kafka")
public class KafkaEndpoint {
    @Inject
    KafkaReceivers receivers;

    @GET
    @Path("/fruits")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<Fruit>> getFruits() {
        return receivers.getFruits();
    }

}
