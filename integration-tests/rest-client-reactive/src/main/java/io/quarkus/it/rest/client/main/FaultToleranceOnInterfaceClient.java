package io.quarkus.it.rest.client.main;

import java.time.temporal.ChronoUnit;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
