package io.quarkus.resteasy.reactive.server.test.duplicate;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello-resteasy")
public class GreetingResource3 {

    @GET
    @Consumes(MediaType.APPLICATION_ATOM_XML)
    public String helloGet(String tutu) {
        return "Hello get";
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String helloPost(String yo) {
        return "Hello post";
    }
}
