package io.quarkus.it.resteasy.reactive.kotlin

import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.delay
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Outgoing

@ApplicationScoped
class CountryNamePayloadTransformer {

    @Incoming("countries-in")
    @Outgoing("countries-t1-out")
    suspend fun transform(countryName: String): String {
        delay(100)
        return countryName.toUpperCase()
    }
}
