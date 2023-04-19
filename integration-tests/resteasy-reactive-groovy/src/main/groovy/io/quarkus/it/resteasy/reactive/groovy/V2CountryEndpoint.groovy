package io.quarkus.it.resteasy.reactive.groovy

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path

@Path("v2")
class V2CountryEndpoint {

    @GET @Path("/name/{name}") def byName(String name) {
        [new Country(name, "$name-capital")]
    }
}
