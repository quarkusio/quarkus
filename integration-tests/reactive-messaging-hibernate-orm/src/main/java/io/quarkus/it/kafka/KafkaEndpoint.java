package io.quarkus.it.kafka;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.it.kafka.fruit.Fruit;
import io.quarkus.it.kafka.people.Person;
import io.quarkus.it.kafka.pet.Pet;

@Path("/kafka")
public class KafkaEndpoint {
    @Inject
    KafkaReceivers receivers;

    @Inject
    @PersistenceUnit("people")
    EntityManager entityManager;

    @GET
    @Path("/fruits")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Fruit> getFruits() {
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
    @Transactional
    public List<Pet> getPets() {
        return receivers.getPets();
    }

    @GET
    @Path("/pets-consumed")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Pet> getConsumedPets() {
        return receivers.getPetsConsumed();
    }

}
