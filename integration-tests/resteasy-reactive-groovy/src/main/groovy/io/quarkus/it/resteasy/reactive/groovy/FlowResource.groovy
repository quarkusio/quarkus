package io.quarkus.it.resteasy.reactive.groovy

import groovy.transform.CompileStatic
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.jboss.resteasy.reactive.ResponseHeader
import org.jboss.resteasy.reactive.ResponseStatus
import org.jboss.resteasy.reactive.RestSseElementType

import java.time.Duration

@CompileStatic
@Path("flow")
class FlowResource {

    @Inject
    private UppercaseService uppercaseService

    @ResponseStatus(201)
    @ResponseHeader(name = "foo", value = ["bar"])
    @GET
    @Path("str")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    Multi<String> sseStrings() {
        Multi.createFrom().emitter(emitter -> {
            try {
                emitter.emit(uppercaseService.convert("Hello"))
                emitter.emit(uppercaseService.convert("From"))
                emitter.emit(uppercaseService.convert("Kotlin"))
                emitter.emit(uppercaseService.convert("Flow"))
            } finally {
                emitter.complete()
            }
        })
    }

    @GET
    @Path("suspendStr")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    Multi<String> suspendSseStrings() {
        Uni.createFrom().nullItem()
            .onItem().delayIt().by(Duration.ofMillis(100))
            .onItem().<String>transformToMulti {
                Multi.createFrom().emitter(emitter -> {
                    try {
                        emitter.emit(uppercaseService.convert("Hello"))
                        emitter.emit(uppercaseService.convert("From"))
                        emitter.emit(uppercaseService.convert("Kotlin"))
                        emitter.emit(uppercaseService.convert("Flow"))
                    } finally {
                        emitter.complete()
                    }
                })
            }
    }

    @GET
    @Path("json")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestSseElementType(MediaType.APPLICATION_JSON)
    Multi<Country> sseJson() {
        Multi.createFrom().items(
            new Country("Barbados", "Bridgetown"), new Country("Mauritius", "Port Louis"),
            new Country("Fiji", "Suva")
        ).onItem().call { Country c -> Uni.createFrom().nullItem().onItem().delayIt().by(Duration.ofSeconds(1)) }
    }
}
