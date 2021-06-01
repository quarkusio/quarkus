package io.quarkus.it.resteasy.reactive.kotlin

import io.smallrye.mutiny.Multi
import org.eclipse.microprofile.reactive.messaging.Outgoing
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class CountryNameProducer {

    @Outgoing("countries-out")
    fun generate(): Multi<String> = Multi.createFrom().items("Greece", "USA", "France")
}