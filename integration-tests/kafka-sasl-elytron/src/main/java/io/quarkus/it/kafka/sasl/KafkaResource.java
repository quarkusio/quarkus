package io.quarkus.it.kafka.sasl;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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