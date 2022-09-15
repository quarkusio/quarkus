package io.quarkus.it.resteasy.reactive.kotlin

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@Path("/v2")
@RegisterRestClient(configKey = "countries")
interface CountriesGateway {

    @GET
    @Path("/name/{name}")
    suspend fun byName(name: String): Set<Country>
}
