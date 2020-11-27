package io.quarkus.it.hibernate.validator.inheritance;

import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

public interface BookResource {

    String PATH = "/books";

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String hello(@NotNull @QueryParam("name") String name);
}