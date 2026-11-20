package io.quarkus.smallrye.openapi.test.jaxrs;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/interface")
public interface AutoTagInterfaceResource {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String hello();
}
