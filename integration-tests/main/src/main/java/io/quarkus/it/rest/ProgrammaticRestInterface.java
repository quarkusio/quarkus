package io.quarkus.it.rest;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;

/**
 * A version of {@link RestInterface} that doesn't have {@link org.eclipse.microprofile.rest.client.inject.RegisterRestClient}
 * and can be used only programmatically, i.e. with the builder.
 */
@Path("/test")
@RegisterClientHeaders
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

    @GET
    @Path("/headers")
    @Produces("application/json")
    Map<String, String> getAllHeaders();
}
