package io.quarkus.resteasy.reactive.jsonb.deployment.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("cheese")
public class CheeseEndpoint {

    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })
    @GET
    public Cheese get() {
        return new Cheese("Morbier");
    }
}
