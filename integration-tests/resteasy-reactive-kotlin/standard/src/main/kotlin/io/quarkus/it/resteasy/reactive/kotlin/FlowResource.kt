package io.quarkus.it.resteasy.reactive.kotlin

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import org.jboss.resteasy.reactive.RestSseElementType
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("flow")
class FlowResource {

    @GET
    @Path("str")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    fun sseStrings() = flow {
        emit("Hello")
        emit("From")
        emit("Kotlin")
        emit("Flow")
    }

    @GET
    @Path("json")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestSseElementType(MediaType.APPLICATION_JSON)
    fun sseJson() = flow {
        emit(Country("Barbados", "Bridgetown"))
        delay(1000)
        emit(Country("Mauritius", "Port Louis"))
        delay(1000)
        emit(Country("Fiji", "Suva"))
    }
}
