package io.quarkus.it.kafka;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.smallrye.reactive.messaging.kafka.Record;

@Path("/kafka")
public class KafkaEndpoint {
    @Inject
    KafkaReceivers receivers;

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

    @GET
    @Path("/data-with-metadata")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getDataWithMetadata() {
        return receivers.getDataWithMetadata();
    }

    @GET
    @Path("/data-for-keyed")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getDataForKeyed() {
        return receivers.getDataForKeyed();
    }
}
