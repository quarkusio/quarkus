package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.smallrye.reactivemessaging.sendSuspending
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import kotlinx.coroutines.delay
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.rest.client.inject.RestClient

@Path("country")
class CountriesEndpoint(
    @RestClient private val countriesGateway: CountriesGateway,
    private val countryNameConsumer: CountryNameConsumer,
    @Channel("countries-emitter") private val countryEmitter: Emitter<String>
) {

    @GET
    @Path("/name/{name}")
    suspend fun byName(name: String): Set<Country> {
        val result = countriesGateway.byName(name)
        delay(50)
        return result
    }

    @POST
    @Path("/kafka/{name}")
    suspend fun sendCountryNameToKafka(name: String): String {
        delay(50)
        countryEmitter.sendSuspending(name)
        return name
    }

    @GET
    @Path("/resolved")
    fun resolvedCountries(): Set<Country> = countryNameConsumer.resolvedCounties
}
