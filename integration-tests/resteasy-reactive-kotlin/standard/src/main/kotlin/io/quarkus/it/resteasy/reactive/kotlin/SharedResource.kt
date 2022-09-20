package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.it.shared.Shared
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces

@Path("/shared")
class SharedResource {

    @Consumes(value = ["application/json", "text/plain"])
    @Produces(value = ["application/json", "text/plain"])
    @POST
    fun returnAsIs(shared: Shared) = shared
}
