package io.quarkus.it.micrometer.prometheus;

import java.time.Duration;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.smallrye.mutiny.Uni;

@Path("/slow")
public class SlowResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> slow() {
        return Uni.createFrom().item("hello").onItem().delayIt().by(Duration.ofSeconds(10));
    }
}
