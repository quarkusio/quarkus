package io.quarkus.it.resteasy.reactive.kotlin

import kotlinx.coroutines.delay
import org.eclipse.microprofile.rest.client.inject.RestClient
import javax.ws.rs.GET
import javax.ws.rs.Path

@Path("country")
class CountriesEndpoint(@RestClient private val countriesService: CountriesService) {

    @GET
    @Path("/name/{name}")
    suspend fun byName(name: String): Set<Country> {
        val result = countriesService.byName(name)
        delay(50)
        return result
    }
}
