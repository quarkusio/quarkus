package io.quarkus.micrometer.test;

import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/hello")
@Singleton
public class HelloResource {
    @GET
    @Path("{message}")
    public String hello(@PathParam("message") String message) {
        return "hello " + message;
    }

    @OPTIONS
    @Path("{message}")
    public String helloOptions(@PathParam("message") String message) {
        return "hello " + message;
    }
}
