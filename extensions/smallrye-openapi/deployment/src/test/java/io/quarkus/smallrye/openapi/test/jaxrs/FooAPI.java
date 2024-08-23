package io.quarkus.smallrye.openapi.test.jaxrs;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/secured/foo")
public interface FooAPI {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Response getFoo();
}