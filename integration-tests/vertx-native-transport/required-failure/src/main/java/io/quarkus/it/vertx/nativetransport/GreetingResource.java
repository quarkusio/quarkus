package io.quarkus.it.vertx.nativetransport;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/greeting")
public class GreetingResource {

    @GET
    public String hello() {
        return "hello";
    }
}
