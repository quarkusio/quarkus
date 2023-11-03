package io.quarkus.jwt.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;

@Path("/authenticated-endpoint")
@Authenticated
public class AuthenticatedEndpoint {

    private final GreetingService greetingService;

    public AuthenticatedEndpoint(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @GET
    @Path("/greet")
    public String greet() {
        return greetingService.greet();
    }
}
