package io.quarkus.it.rest.client.main;

import java.time.temporal.ChronoUnit;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/unprocessable")
@RegisterRestClient(configKey = "w-fault-tolerance")
@CircuitBreaker(requestVolumeThreshold = 2, delay = 1, delayUnit = ChronoUnit.MINUTES)
public interface FaultToleranceOnInterfaceClient {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    String hello();
}
