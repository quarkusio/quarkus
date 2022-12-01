package io.quarkus.it.mockbean;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
