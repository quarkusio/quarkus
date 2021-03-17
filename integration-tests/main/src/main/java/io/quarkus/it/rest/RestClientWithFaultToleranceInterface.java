package io.quarkus.it.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Used to test {@link Fallback} on default methods on interfaces.
 */
@RegisterRestClient
@Path("/test")
public interface RestClientWithFaultToleranceInterface {

    @GET
    @Path("/failure")
    @Fallback(fallbackMethod = "fallback")
    String echo();

    default String fallback() {
        return "Hello fallback!";
    }
}
