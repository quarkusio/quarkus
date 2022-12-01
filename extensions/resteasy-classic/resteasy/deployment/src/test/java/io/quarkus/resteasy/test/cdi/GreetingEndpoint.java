package io.quarkus.resteasy.test.cdi;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
