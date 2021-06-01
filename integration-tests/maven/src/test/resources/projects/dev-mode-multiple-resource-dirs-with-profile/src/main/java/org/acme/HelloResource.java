package org.acme;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
