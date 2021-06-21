package io.quarkus.rest.client.reactive.stork;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/hello")
public class HelloResource {

    public static final String HELLO_WORLD = "Hello, World!";

    @GET
    public String hello() {
        return HELLO_WORLD;
    }
}
