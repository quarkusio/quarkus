package io.quarkus.it.resteasy.reactive.kotlin

import io.smallrye.mutiny.Multi
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Outgoing

@ApplicationScoped
class CountryNameProducer {

    @Outgoing("countries-out")
    fun generate(): Multi<String> = Multi.createFrom().items("Greece", "USA", "France")
}
