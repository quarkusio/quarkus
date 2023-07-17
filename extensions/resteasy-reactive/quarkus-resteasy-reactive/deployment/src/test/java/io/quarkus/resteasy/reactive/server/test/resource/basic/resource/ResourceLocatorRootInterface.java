package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Produces(MediaType.TEXT_PLAIN)
public interface ResourceLocatorRootInterface {

    @GET
    String get();

    @Path("{id}")
    Object getSubSubResource(@PathParam("id") String id);
}
