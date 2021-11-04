package io.quarkus.smallrye.openapi.test.jaxrs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Produces(MediaType.TEXT_PLAIN)
public interface AbstractAutoTagResource<T> {
    @GET
    @Path("/{id}")
    T getById(@PathParam("id") long id);
}
