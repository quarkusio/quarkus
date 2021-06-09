package io.quarkus.micrometer.test;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

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
