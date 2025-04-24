package io.quarkus.it.faulttolerance;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/ft")
public class FaultToleranceTestResource {

    @Inject
    Service service;

    @Inject
    SecondService secondService;

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

    @GET
    @Path("/fallback")
    public String fallback() {
        AtomicInteger counter = new AtomicInteger();
        String name = service.fallbackMethod(counter);
        return counter + ":" + name;
    }

    @GET
    @Path("/hello")
    public String hello() {
        return secondService.publicHello();
    }
}
