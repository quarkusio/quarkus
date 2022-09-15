package io.quarkus.resteasy.test;

import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Singleton
@Path("/ctor-single")
public class SingletonConstructorInjectionResource {

    final Service service;

    public SingletonConstructorInjectionResource(Service service) {
        this.service = service;
    }

    @GET
    public String val() {
        return service.execute();
    }
}
