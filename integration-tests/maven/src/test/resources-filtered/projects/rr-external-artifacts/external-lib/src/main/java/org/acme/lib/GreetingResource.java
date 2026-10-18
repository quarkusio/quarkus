package org.acme.lib;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/greet")
class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String greet() {
        return "Hello from Quarkus REST";
    }
}