package io.quarkus.it.resteasy.reactive.kotlin

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path

@Path("v2")
class V2CountryEndpoint {

    @GET
    @Path("/name/{name}")
    fun byName(name: String) = listOf(Country(name, "$name-capital"))
}
