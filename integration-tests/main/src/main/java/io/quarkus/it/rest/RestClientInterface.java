package io.quarkus.it.rest;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Used to test default @{@link Dependent} scope defined on interface
 */
@RegisterRestClient
@Path("/test")
@RegisterClientHeaders
public interface RestClientInterface {

    @GET
    @Path("/echo/{echo}")
    String echo(@PathParam("echo") String echo);

    @GET
    String get();

    @GET
    @Path("/jackson")
    @Produces("application/json")
    TestResource.MyData getData();
}
