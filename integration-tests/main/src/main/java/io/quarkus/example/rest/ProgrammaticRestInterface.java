package io.quarkus.example.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * A version of {@link RestInterface} that doesn't have {@link org.eclipse.microprofile.rest.client.inject.RegisterRestClient}
 * and can be used only programmatically, i.e. with the builder.
 */
@Path("/test")
public interface ProgrammaticRestInterface {

    @GET
    String get();

    @GET
    @Path("/jackson")
    @Produces("application/json")
    TestResource.MyData getData();

    @GET
    @Path("/complex")
    @Produces("application/json")
    List<ComponentType> complex();
}
