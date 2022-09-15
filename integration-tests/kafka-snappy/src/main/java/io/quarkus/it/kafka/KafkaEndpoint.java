package io.quarkus.it.kafka;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/kafka")
public class KafkaEndpoint {

    @Inject
    KafkaProducerManager manager;

    @Inject
    KafkaConsumerManager consumer;

    @POST
    public void post(String message) {
        manager.send(message);
    }

    @GET
    public String get() {
        return consumer.receive();
    }
}
