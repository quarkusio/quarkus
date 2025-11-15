package io.quarkus.rest.client.reactive.stork;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;

@Path("/hello")
public class HelloResource {

    public static final String HELLO_WORLD = "Hello, World!";

    @GET
    public String hello() {
        return HELLO_WORLD;
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public String invoke(@PathParam("name") String name) {
        return "Hello, " + name;
    }

    @GET
    @Path("/query")
    public String helloWithQueryParam(@QueryParam("foo") String foo) {
        return "Hello, this is your query parameter: " + foo;
    }

    @POST
    public String echo(String name, @Context Request request) {
        return "hello, " + name;
    }
}
