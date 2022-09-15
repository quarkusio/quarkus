package org.acme;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class HelloResource {

    @ConfigProperty(name = "greeting")
    String greeting;

    @ConfigProperty(name = "other.greeting", defaultValue = "Other")
    String otherGreeting;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }

    @GET
    @Path("/greetings")
    @Produces(MediaType.TEXT_PLAIN)
    public String greetings() {
        return greeting + "/" + otherGreeting;
    }
}
