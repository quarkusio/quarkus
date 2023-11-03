package io.quarkus.it.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

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
