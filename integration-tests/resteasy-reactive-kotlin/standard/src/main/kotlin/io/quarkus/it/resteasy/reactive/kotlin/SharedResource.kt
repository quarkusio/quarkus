package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.it.shared.Shared
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces

@Path("/shared")
class SharedResource {

    @Consumes(value = ["application/json", "text/plain"])
    @Produces(value = ["application/json", "text/plain"])
    @POST
    fun returnAsIs(shared: Shared) = shared
}
