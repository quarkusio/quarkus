package io.quarkus.it.resteasy.reactive.kotlin

import kotlinx.coroutines.delay
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import org.eclipse.microprofile.reactive.messaging.Outgoing
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class CountryNameMessageTransformer {

    @Incoming("countries-t1-in")
    @Outgoing("countries-t2-out")
    suspend fun transform(input: Message<String>): Message<String> {
//    suspend fun transform(input: String): String {
        delay(100)
        return input.withPayload(input.payload.toLowerCase())
//        return input.toLowerCase()
    }
}