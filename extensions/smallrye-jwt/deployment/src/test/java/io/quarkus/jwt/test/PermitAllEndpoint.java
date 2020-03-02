package io.quarkus.jwt.test;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
