package io.quarkus.it.resteasy.reactive.kotlin

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Path("enum")
class EnumEndpoint {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun test(@QueryParam(value = "state") states: List<State>) = "States: $states"

}
