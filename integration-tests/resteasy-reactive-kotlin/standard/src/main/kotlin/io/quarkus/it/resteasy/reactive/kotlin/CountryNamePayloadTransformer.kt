package io.quarkus.it.resteasy.reactive.kotlin

import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.delay
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Outgoing

@ApplicationScoped
class CountryNamePayloadTransformer {

    @Incoming("countries-in")
    @Outgoing("countries-t1-out")
    suspend fun transform(country: Country): Country {
        delay(100)
        return Country(country.name.uppercase(), country.capital)
    }
}
