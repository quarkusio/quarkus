package io.quarkus.it.rest.client.main;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/unprocessable")
@RegisterRestClient(configKey = "w-fault-tolerance")
public interface FaultToleranceClient {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    String hello();

    @Fallback(fallbackMethod = "fallback")
    default String helloWithFallback() {
        return hello();
    }

    default String fallback() {
        return "Hello fallback!";
    }
}
