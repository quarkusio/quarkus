package io.quarkus.resteasy.reactive.jackson.deployment.test;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/kotlin-json-property")
public class KotlinJsonPropertyResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public KotlinJsonPropertyDto get() {
        return new KotlinJsonPropertyDto("Alice");
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public KotlinJsonPropertyDto echo(KotlinJsonPropertyDto dto) {
        return dto;
    }
}
