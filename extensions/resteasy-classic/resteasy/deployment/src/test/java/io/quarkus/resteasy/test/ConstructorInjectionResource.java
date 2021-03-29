package io.quarkus.resteasy.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/ctor")
public class ConstructorInjectionResource {

    final Service service;

    public ConstructorInjectionResource(Service service) {
        this.service = service;
    }

    @GET
    public String val() {
        return service.execute();
    }
}
