package io.quarkus.it.resteasy.reactive.groovy

import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Outgoing

import java.time.Duration

@ApplicationScoped
class CountryNamePayloadTransformer {

    @Incoming("countries-in")
    @Outgoing("countries-t1-out")
    Uni<String> transform(String countryName) {
        Uni.createFrom().item(countryName)
                .onItem().transform {it.toUpperCase()}
                .onItem().delayIt().by(Duration.ofMillis(100))
    }
}
