package io.quarkus.jwt.test;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/permit-all-endpoint")
@PermitAll
public class PermitAllEndpoint {

    private final GreetingService greetingService;

    public PermitAllEndpoint(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @GET
    @Path("/greet")
    public String greet() {
        return greetingService.greet();
    }
}
