package io.quarkus.resteasy.reactive.jackson.deployment.test;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/global-naming")
public class GlobalNamingResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String echo(GlobalNamingRequest request) {
        return "{\"values\":\"" + request.firstName() + " " + request.lastName() + " " + request.yearsOld() + "\"}";
    }

    @GET
    @Path("/ser")
    @Produces(MediaType.APPLICATION_JSON)
    public GlobalNamingRequest get() {
        return new GlobalNamingRequest("Alice", "Smith", 30);
    }
}
