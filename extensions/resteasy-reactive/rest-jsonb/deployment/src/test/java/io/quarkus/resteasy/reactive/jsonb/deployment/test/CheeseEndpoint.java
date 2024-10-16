package io.quarkus.resteasy.reactive.jsonb.deployment.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("cheese")
public class CheeseEndpoint {

    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })
    @GET
    public Cheese get() {
        return new Cheese("Morbier");
    }
}
