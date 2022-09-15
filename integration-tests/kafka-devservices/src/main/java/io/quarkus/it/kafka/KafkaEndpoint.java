package io.quarkus.it.kafka;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
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
}
