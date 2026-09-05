package io.quarkus.resteasy.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Hello
@Path("/ctor-namebinding")
public class ClassLevelNameBindingResource {

    final Service service;

    public ClassLevelNameBindingResource(Service service) {
        this.service = service;
    }

    @GET
    public String val() {
        return service.execute();
    }
}
