package io.quarkus.it.resteasy.reactive.kotlin

import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.delay
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import org.eclipse.microprofile.reactive.messaging.Outgoing

@ApplicationScoped
class CountryNameMessageTransformer {

    @Incoming("countries-t1-in")
    @Outgoing("countries-t2-out")
    suspend fun transform(input: Message<Country>): Message<String> {
        delay(100)
        return input.withPayload(input.payload.name.lowercase())
    }
}
