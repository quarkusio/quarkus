package io.quarkus.resteasy.reactive.server.test.duplicate;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
