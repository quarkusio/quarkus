package org.acme.app;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
