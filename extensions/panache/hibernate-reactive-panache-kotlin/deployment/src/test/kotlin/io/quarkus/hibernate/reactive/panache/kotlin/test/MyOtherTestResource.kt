package io.quarkus.hibernate.reactive.panache.kotlin.test

import io.smallrye.mutiny.Uni
import java.util.function.Supplier
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("other-entity")
class MyOtherTestResource {
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun get(@PathParam("id") id: Long): Uni<MyOtherEntity> {
        return MyOtherEntity
            .findById(id)
            .onItem().ifNull().failWith { WebApplicationException(Response.Status.NOT_FOUND) }
    }
}