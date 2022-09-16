package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.it.shared.Shared
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces

@Path("/shared")
class SharedResource {

    @Consumes("application/json")
    @Produces("application/json")
    @POST
    fun returnAsIs(shared: Shared) = shared
}
