package io.quarkus.it.rest;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
