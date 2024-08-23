package io.quarkus.it.kafka;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.RedisDataSource;
import io.smallrye.reactive.messaging.kafka.Record;
import io.smallrye.reactive.messaging.kafka.commit.ProcessingState;

@Path("/kafka")
public class KafkaEndpoint {
    @Inject
    KafkaReceivers receivers;

    @Inject
    @RedisClientName("my-redis")
    RedisDataSource rds;

    @GET
    @Path("/people")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Person> getPeople() {
        return receivers.getPeople();
    }

    @GET
    @Path("/people-state/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public ProcessingState<KafkaReceivers.PeopleState> getPeopleState(@PathParam("key") String key) {
        return (ProcessingState<KafkaReceivers.PeopleState>) rds.value(ProcessingState.class).get(key);
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
