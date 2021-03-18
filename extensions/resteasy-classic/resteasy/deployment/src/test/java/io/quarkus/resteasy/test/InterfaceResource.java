package io.quarkus.resteasy.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/inter")
public interface InterfaceResource {

    @Path("/hello")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String hello();
}
