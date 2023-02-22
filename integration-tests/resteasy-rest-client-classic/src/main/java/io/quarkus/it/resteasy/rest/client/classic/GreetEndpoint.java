package io.quarkus.it.resteasy.rest.client.classic;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

public interface GreetEndpoint {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Valid
    Greet greet(
            @QueryParam("who") @DefaultValue("Person") @Pattern(regexp = "^[A-Z][a-z]+$") String name);
}
