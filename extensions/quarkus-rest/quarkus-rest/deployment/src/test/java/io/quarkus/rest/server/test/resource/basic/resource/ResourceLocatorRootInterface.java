package io.quarkus.rest.server.test.resource.basic.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Produces(MediaType.TEXT_PLAIN)
public interface ResourceLocatorRootInterface {

    @GET
    String get();

    @Path("{id}")
    Object getSubSubResource(@PathParam("id") String id);
}
