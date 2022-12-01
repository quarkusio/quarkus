package io.quarkus.it.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/otherTest/{name:[a-z]{1,10}}")
public class TestResourceWithVariable {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@PathParam("name") String name) {
        return "hello " + name;
    }
}
