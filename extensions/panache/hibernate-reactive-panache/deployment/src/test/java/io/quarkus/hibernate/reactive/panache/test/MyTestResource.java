package io.quarkus.hibernate.reactive.panache.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.smallrye.mutiny.Uni;

@Path("entity")
public class MyTestResource {

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MyEntity> get(@PathParam long id) {
        return MyEntity.<MyEntity> findById(id)
                .onItem().ifNull().failWith(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }
}
