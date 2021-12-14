package io.quarkus.it.kafka;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.smallrye.reactive.messaging.kafka.Record;

@Path("/kafka")
public class KafkaEndpoint {
    @Inject
    KafkaReceivers receivers;

    @GET
    @Path("/people")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Person> getPeople() {
        return receivers.getPeople();
    }

    @GET
    @Path("/fruits")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Fruit> getFruits() {
        return receivers.getFruits();
    }

    @GET
    @Path("/pets")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Pet> getPets() {
        return receivers.getPets().stream().map(Record::key).collect(Collectors.toList());
    }
}
