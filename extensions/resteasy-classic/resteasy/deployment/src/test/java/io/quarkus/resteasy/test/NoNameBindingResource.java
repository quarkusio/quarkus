package io.quarkus.resteasy.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/no-namebinding")
public class NoNameBindingResource {

    final Service service;

    public NoNameBindingResource(Service service) {
        this.service = service;
    }

    @GET
    public String val() {
        return service.execute();
    }
}
