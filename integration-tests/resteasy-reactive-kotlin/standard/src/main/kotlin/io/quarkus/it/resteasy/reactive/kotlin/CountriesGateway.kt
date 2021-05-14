package io.quarkus.it.resteasy.reactive.kotlin

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import javax.ws.rs.GET
import javax.ws.rs.Path

@Path("/v2")
@RegisterRestClient(configKey = "countries")
interface CountriesGateway {

    @GET
    @Path("/name/{name}")
    suspend fun byName(name: String): Set<Country>
}
