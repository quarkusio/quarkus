package io.quarkus.it.mockbean;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/dummy")
public class DummyResource {

    @Inject
    @Named("first")
    DummyService firstDummyService;

    @Inject
    @Named("second")
    DummyService secondDummyService;

    @GET
    public String firstPlusSecond() {
        return firstDummyService.returnDummyValue() + "/" + secondDummyService.returnDummyValue();
    }
}
