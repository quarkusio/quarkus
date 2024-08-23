package io.quarkus.virtual.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/api")
public class ServiceApi {

    @GET
    public Greeting hello() {
        Greeting greeting = new Greeting();
        greeting.message = "hello";
        return greeting;
    }

}
