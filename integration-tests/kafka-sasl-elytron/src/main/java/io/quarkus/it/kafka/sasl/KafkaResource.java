package io.quarkus.it.kafka.sasl;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/kafka")
public class KafkaResource {

    @Inject
    KafkaConsumer consumer;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getValues() {
        return consumer.getValues();
    }
}
