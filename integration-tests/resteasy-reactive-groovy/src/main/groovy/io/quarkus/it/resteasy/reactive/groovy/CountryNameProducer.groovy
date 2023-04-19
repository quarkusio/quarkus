package io.quarkus.it.resteasy.reactive.groovy

import groovy.transform.CompileStatic
import io.smallrye.mutiny.Multi
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Outgoing

@CompileStatic
@ApplicationScoped
class CountryNameProducer {

    @Outgoing("countries-out")
    Multi<String> generate() {
        Multi.createFrom().items("Greece", "USA", "France")
    }
}
