package io.quarkus.resteasy.reactive.jackson.deployment.test;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/kotlin-boolean-is-prefix")
public class KotlinBooleanIsPrefixResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public KotlinBooleanIsPrefixDto get() {
        return new KotlinBooleanIsPrefixDto(true);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public KotlinBooleanIsPrefixDto echo(KotlinBooleanIsPrefixDto dto) {
        return dto;
    }
}
