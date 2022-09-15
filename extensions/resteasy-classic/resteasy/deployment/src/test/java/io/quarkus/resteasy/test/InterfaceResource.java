package io.quarkus.resteasy.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/inter")
public interface InterfaceResource {

    @Path("/hello")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String hello();
}
