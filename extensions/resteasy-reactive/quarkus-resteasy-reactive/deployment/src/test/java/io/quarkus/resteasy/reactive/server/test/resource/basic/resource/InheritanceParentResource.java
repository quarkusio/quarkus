package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;

public interface InheritanceParentResource {

    @GET
    @Produces("text/plain")
    String firstest();

}
