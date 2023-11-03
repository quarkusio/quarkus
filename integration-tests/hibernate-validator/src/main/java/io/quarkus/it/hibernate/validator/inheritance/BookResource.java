package io.quarkus.it.hibernate.validator.inheritance;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

public interface BookResource {

    String PATH = "/books";

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String hello(@NotNull @QueryParam("name") String name);
}
