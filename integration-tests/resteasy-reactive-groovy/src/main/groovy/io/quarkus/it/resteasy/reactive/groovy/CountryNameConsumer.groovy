package io.quarkus.it.resteasy.reactive.groovy

import groovy.transform.CompileStatic
import io.quarkus.runtime.annotations.RegisterForReflection
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.rest.client.inject.RestClient

import java.util.concurrent.ConcurrentHashMap

@RegisterForReflection // Needed to allow the closure accessing {@code resolvedCounties}
@CompileStatic
@ApplicationScoped
class CountryNameConsumer {

    @Inject
    @RestClient
    private CountriesGateway countryGateway

    Set<Country> resolvedCounties = ConcurrentHashMap.newKeySet()

    @Incoming("countries-t2-in")
    Uni<Void> consume(String countryName) {
        Uni.combine().all().unis(countryGateway.byName("fake$countryName"), countryGateway.byName(countryName))
            .asTuple().onItem().transform {
                resolvedCounties.addAll(it.getItem1())
                resolvedCounties.addAll(it.getItem2())
            }
            .replaceWithVoid()
    }
}
