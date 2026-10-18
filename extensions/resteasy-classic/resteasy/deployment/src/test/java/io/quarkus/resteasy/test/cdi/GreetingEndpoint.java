package io.quarkus.resteasy.test.cdi;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 *
 */
@Path("/cdi-greeting")
public class GreetingEndpoint {

    @Inject
    Greeting greeting;

    @GET
    @Path("/greet")
    public String greet() {
        return greeting.greet();
    }
}
