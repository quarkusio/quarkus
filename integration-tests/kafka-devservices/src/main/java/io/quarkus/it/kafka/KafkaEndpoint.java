package io.quarkus.it.kafka;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

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
