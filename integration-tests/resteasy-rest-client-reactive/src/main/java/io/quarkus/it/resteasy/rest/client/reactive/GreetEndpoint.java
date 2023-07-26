package io.quarkus.it.resteasy.rest.client.reactive;

import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

public interface GreetEndpoint {
    // FIXME: Hibernate Validator doesn't like the @Valid annotation here,
    //        as that's also used as a reactive REST client. Instead, it is
    //        moved to the implementation bean.
    @GET
    @Path("") // FIXME: this @Path shouln't be needed, but it is
    @Produces(MediaType.APPLICATION_JSON)
    Greet greet(
            @QueryParam("who") @DefaultValue("Person") @Pattern(regexp = "^[A-Z][a-z]+$") String name);
}
