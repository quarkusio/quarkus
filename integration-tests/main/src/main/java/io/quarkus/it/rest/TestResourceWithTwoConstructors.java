package io.quarkus.it.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/testCtor2")
public class TestResourceWithTwoConstructors {

    Someservice service;

    public TestResourceWithTwoConstructors() {
    }

    @Inject
    public TestResourceWithTwoConstructors(Someservice service) {
        this.service = service;
    }

    @GET
    @Path("/service")
    public String service() {
        return service.name();
    }
}
