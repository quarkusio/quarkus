package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("poly")
public class PolymorphicEndpoint {

    @Produces(MediaType.APPLICATION_JSON)
    @GET
    @Path("single")
    public PolymorphicBase getSingle() {
        return new PolymorphicSub();
    }

    @Produces(MediaType.APPLICATION_JSON)
    @GET
    @Path("many")
    public List<PolymorphicBase> getMany() {
        return List.of(new PolymorphicSub(), new PolymorphicSub());
    }
}
