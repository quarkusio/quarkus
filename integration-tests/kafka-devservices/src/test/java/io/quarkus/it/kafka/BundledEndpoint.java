package io.quarkus.it.kafka;

import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/kafka")
public class BundledEndpoint {

    @Inject
    KafkaAdminManager admin;

    @GET
    @Path("/partitions/{topic}")
    public Integer partitions(@PathParam("topic") String topic) {
        return admin.partitions(topic);
    }

    @GET
    @Path("/port")
    public Integer port() throws ExecutionException, InterruptedException {
        return admin.port();
    }
}
