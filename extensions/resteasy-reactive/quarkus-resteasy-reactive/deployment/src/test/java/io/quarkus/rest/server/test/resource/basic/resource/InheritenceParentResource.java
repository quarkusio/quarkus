package io.quarkus.rest.server.test.resource.basic.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;

public interface InheritenceParentResource {

    @GET
    @Produces("text/plain")
    String firstest();

}
