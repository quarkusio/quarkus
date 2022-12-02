package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;

public interface InheritanceParentResource {

    @GET
    @Produces("text/plain")
    String firstest();

}
