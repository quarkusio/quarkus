package io.quarkus.it.resteasy.reactive.kotlin

import io.smallrye.mutiny.Multi
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Outgoing

@ApplicationScoped
class CountryNameProducer {

    @Outgoing("countries-out")
    fun generate(): Multi<Country> =
        Multi.createFrom()
            .items(
                Country("Greece", "Athens"),
                Country("USA", "Washington D.C"),
                Country("France", "Paris"),
            )
}
