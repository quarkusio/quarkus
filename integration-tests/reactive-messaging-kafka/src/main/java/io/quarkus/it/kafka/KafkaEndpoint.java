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

    @Inject
    ExactlyOnceProcessor exactlyOnceProcessor;

    @Inject
    ExactlyOnceConsumer exactlyOnceConsumer;

    @Inject
    ExactlyOnceListProcessor exactlyOnceListProcessor;

    @Inject
    ExactlyOnceListConsumer exactlyOnceListConsumer;

    @Inject
    ExactlyOnceUniProcessor exactlyOnceUniProcessor;

    @Inject
    ExactlyOnceUniConsumer exactlyOnceUniConsumer;

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

    @GET
    @Path("/exactly-once-processed")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Integer> getExactlyOnceProcessed() {
        return exactlyOnceProcessor.getProcessed();
    }

    @GET
    @Path("/exactly-once-results")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Integer> getExactlyOnceResults() {
        return exactlyOnceConsumer.getResults();
    }

    @GET
    @Path("/exactly-once-list-processed")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Integer> getExactlyOnceListProcessed() {
        return exactlyOnceListProcessor.getProcessed();
    }

    @GET
    @Path("/exactly-once-list-results")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Integer> getExactlyOnceListResults() {
        return exactlyOnceListConsumer.getResults();
    }

    @GET
    @Path("/exactly-once-uni-processed")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Integer> getExactlyOnceUniProcessed() {
        return exactlyOnceUniProcessor.getProcessed();
    }

    @GET
    @Path("/exactly-once-uni-results")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Integer> getExactlyOnceUniResults() {
        return exactlyOnceUniConsumer.getResults();
    }
}
