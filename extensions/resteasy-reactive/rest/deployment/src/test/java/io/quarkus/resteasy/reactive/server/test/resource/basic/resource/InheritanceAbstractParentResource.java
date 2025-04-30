package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/inheritance-abstract-parent-test")
public abstract class InheritanceAbstractParentResource {

    @GET
    public abstract String get();

}
