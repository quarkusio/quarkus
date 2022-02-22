package io.quarkus.it.kafka;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
