package io.quarkus.it.faulttolerance;

import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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

}
