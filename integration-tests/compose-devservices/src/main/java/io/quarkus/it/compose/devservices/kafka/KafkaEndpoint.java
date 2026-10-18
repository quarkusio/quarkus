package io.quarkus.it.compose.devservices.kafka;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/kafka")
public class KafkaEndpoint {

    @Inject
    KafkaAdminManager admin;

    @GET
    @Path("/partitions/{topic}")
    public Integer partitions(@PathParam("topic") String topic) {
        return admin.partitions(topic);
    }

    @PUT
    @Path("/topics/{topic}/{partitions}")
    public void partitions(@PathParam("topic") String topic, @PathParam("partitions") int partitions) {
        admin.createTopic(topic, partitions);
    }
}
