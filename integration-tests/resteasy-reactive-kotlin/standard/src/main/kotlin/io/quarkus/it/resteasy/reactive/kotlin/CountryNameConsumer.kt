package io.quarkus.it.resteasy.reactive.kotlin

import jakarta.enterprise.context.ApplicationScoped
import java.util.concurrent.ConcurrentHashMap
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.rest.client.inject.RestClient

@ApplicationScoped
class CountryNameConsumer(@RestClient private val countryGateway: CountriesGateway) {

    val resolvedCounties: MutableSet<Country> = ConcurrentHashMap.newKeySet()

    @Incoming("countries-t2-in")
    suspend fun consume(countryName: String) {
        resolvedCounties.addAll(countryGateway.byName("fake$countryName"))
        resolvedCounties.addAll(countryGateway.byName(countryName))
    }
}
