package io.quarkus.resteasy.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/method-namebinding")
public class MethodLevelNameBindingResource {

    final Service service;

    public MethodLevelNameBindingResource(Service service) {
        this.service = service;
    }

    @Hello
    @GET
    public String val() {
        return service.execute();
    }
}
