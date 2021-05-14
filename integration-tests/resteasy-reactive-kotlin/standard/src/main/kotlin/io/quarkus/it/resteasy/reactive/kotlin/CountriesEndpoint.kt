package io.quarkus.it.resteasy.reactive.kotlin

import kotlinx.coroutines.delay
import org.eclipse.microprofile.rest.client.inject.RestClient
import javax.ws.rs.GET
import javax.ws.rs.Path

@Path("country")
class CountriesEndpoint(@RestClient private val countriesGateway: CountriesGateway,
                        private val countryNameConsumer: CountryNameConsumer) {

    @GET
    @Path("/name/{name}")
    suspend fun byName(name: String): Set<Country> {
        val result = countriesGateway.byName(name)
        delay(50)
        return result
    }

    @GET
    @Path("/resolved")
    fun resolvedCountries(): Set<Country> = countryNameConsumer.resolvedCounties
}
