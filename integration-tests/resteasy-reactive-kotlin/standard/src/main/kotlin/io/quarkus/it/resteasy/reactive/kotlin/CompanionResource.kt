package io.quarkus.it.resteasy.reactive.kotlin

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/companion")
class CompanionResource {
    @Path("success")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun success() = ResponseData.success()

    @Path("failure")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun failure() = ResponseData.failure("error")
}
