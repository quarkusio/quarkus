package io.quarkus.it.kafka;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.reactive.mutiny.Mutiny;

import io.smallrye.mutiny.Uni;

@Path("/kafka")
public class KafkaEndpoint {
    @Inject
    KafkaReceivers receivers;

    @Inject
    Mutiny.SessionFactory sf;

    @GET
    @Path("/fruits")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<Fruit>> getFruits() {
        return receivers.getFruits();
    }

    @GET
    @Path("/people")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Person> getPeople() {
        return receivers.getPeople();
    }

    @GET
    @Path("/pets")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<Pet>> getPets() {
        return receivers.getPets();
    }

    @GET
    @Path("/pets-consumed")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Pet> getConsumedPets() {
        return receivers.getConsumedPets();
    }

}
