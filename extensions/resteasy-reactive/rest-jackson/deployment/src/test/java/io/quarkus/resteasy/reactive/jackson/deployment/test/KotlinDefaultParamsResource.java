package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/kotlin-default-params")
public class KotlinDefaultParamsResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public KotlinDefaultParamsDto get() {
        return new KotlinDefaultParamsDto("Alice", "Hello", List.of());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public KotlinDefaultParamsDto echo(KotlinDefaultParamsDto dto) {
        return dto;
    }
}
