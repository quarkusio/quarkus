package io.quarkus.it.rest;

import javax.enterprise.context.Dependent;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

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
    String get();

    @GET
    @Path("/jackson")
    @Produces("application/json")
    TestResource.MyData getData();

    @GET
    @Path("/slow")
    String getSlowAnswer();
}
