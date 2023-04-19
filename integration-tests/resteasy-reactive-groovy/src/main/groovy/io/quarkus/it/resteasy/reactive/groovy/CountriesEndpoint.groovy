package io.quarkus.it.resteasy.reactive.groovy

import groovy.transform.CompileStatic
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.rest.client.inject.RestClient

import java.time.Duration

@CompileStatic
@Path("country")
class CountriesEndpoint {

    @Inject
    @RestClient
    private CountriesGateway countriesGateway
    @Inject
    private CountryNameConsumer countryNameConsumer
    @Inject
    @Channel("countries-emitter")
    private Emitter<String> countryEmitter

    @GET
    @Path("/name/{name}")
    Uni<Set<Country>> byName(String name) {
        countriesGateway.byName(name).onItem().delayIt().by(Duration.ofMillis(50))
    }

    @POST
    @Path("/kafka/{name}")
    Uni<String> sendCountryNameToKafka(String name) {
        Uni.createFrom().completionStage(countryEmitter.send(name))
            .onItem().delayIt().by(Duration.ofMillis(50))
            .replaceWith(name);
    }

    @GET
    @Path("/resolved")
    Set<Country> resolvedCountries() {
        countryNameConsumer.resolvedCounties
    }
}
