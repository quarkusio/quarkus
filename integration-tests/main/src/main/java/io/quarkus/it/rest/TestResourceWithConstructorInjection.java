package io.quarkus.it.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/testCtor")
public class TestResourceWithConstructorInjection {

    final Someservice service;

    public TestResourceWithConstructorInjection(Someservice service) {
        this.service = service;
    }

    @GET
    @Path("/service")
    public String service() {
        return service.name();
    }
}
