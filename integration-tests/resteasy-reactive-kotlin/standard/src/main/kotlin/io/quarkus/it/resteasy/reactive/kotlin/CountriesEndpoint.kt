package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.smallrye.reactivemessaging.sendSuspending
import kotlinx.coroutines.delay
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.rest.client.inject.RestClient
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path

@Path("country")
class CountriesEndpoint(@RestClient private val countriesGateway: CountriesGateway,
                        private val countryNameConsumer: CountryNameConsumer,
                        @Channel("countries-emitter") private val countryEmitter: Emitter<String>) {

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
