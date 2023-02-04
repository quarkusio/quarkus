package io.quarkus.it.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/otherTest/{name:[a-z]{1,10}}")
public class TestResourceWithVariable {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@PathParam("name") String name) {
        return "hello " + name;
    }
}
