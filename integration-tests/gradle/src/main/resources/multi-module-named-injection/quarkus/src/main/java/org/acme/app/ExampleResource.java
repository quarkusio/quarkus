package org.acme.app;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class ExampleResource {

    @Inject
    Service1 service1;

    @Inject
    Service2 service2;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello " + service1.getHealthStatus().isHealthy() + " " + service2.getHealthStatus().isHealthy();
    }
}
