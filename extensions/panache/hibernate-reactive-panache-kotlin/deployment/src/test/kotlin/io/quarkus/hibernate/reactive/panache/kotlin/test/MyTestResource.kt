package io.quarkus.hibernate.reactive.panache.kotlin.test

import io.smallrye.mutiny.Uni
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("entity")
class MyTestResource {
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun get(@PathParam("id") id: Long): Uni<MyTestEntity> {
        return MyTestEntity.findById(
            id
        )
            .onItem().ifNull().failWith{WebApplicationException(Response.Status.NOT_FOUND) }
    }
}