package io.quarkus.hibernate.reactive.panache.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.smallrye.mutiny.Uni;

@Path("other-entity")
public class MyOtherTestResource {

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MyOtherEntity> get(@PathParam("id") long id) {
        return MyOtherEntity.<MyOtherEntity> findById(id)
                .onItem().ifNull().failWith(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }
}
