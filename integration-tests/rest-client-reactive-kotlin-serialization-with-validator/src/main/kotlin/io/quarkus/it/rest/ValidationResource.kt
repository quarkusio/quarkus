package io.quarkus.it.rest

import jakarta.validation.constraints.Size
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/")
class ValidationResource {

    @GET
    @Path("/validate/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun validate(
        @Size(min = 5, message = "string is too short") @PathParam("id") id: String?
    ): String? {
        return id
    }
}
