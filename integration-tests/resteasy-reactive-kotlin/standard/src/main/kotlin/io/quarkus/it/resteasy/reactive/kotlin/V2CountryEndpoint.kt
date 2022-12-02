package io.quarkus.it.resteasy.reactive.kotlin

import javax.ws.rs.GET
import javax.ws.rs.Path

@Path("v2")
class V2CountryEndpoint {

    @GET
    @Path("/name/{name}")
    fun byName(name: String) = listOf(Country(name, "$name-capital"))
}
