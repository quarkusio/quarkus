package io.quarkus.it.resteasy.reactive.groovy

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Path("enum")
class EnumEndpoint {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    def test(@QueryParam(value = "state") List<State> states) {
        "States: $states"
    }
}
