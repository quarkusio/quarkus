package io.quarkus.it.faulttolerance;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/ft")
public class FaultToleranceTestResource {

    @Inject
    Service service;

    @GET
    public String getName() {
        AtomicInteger counter = new AtomicInteger();
        String name = service.getName(counter);
        return counter + ":" + name;
    }

    @GET
    @Path("/retried")
    public String retried() {
        AtomicInteger counter = new AtomicInteger();
        String name = service.retriedMethod(counter);
        return counter + ":" + name;
    }

}
