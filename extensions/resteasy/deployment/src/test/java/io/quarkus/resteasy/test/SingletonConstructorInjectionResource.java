package io.quarkus.resteasy.test;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
