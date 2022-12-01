package io.quarkus.resteasy.reactive.jackson.deployment.test;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("fromage")
public class FroMageEndpoint {

    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PUT
    public FroMage putJson(FroMage fromage) {
        return fromage;
    }
}
