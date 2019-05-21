package io.quarkus.it.kafka;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

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
