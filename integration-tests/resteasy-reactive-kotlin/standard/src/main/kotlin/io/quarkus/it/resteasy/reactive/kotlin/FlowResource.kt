package io.quarkus.it.resteasy.reactive.kotlin

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jboss.resteasy.reactive.ResponseHeader
import org.jboss.resteasy.reactive.ResponseStatus
import org.jboss.resteasy.reactive.RestSseElementType

@Path("flow")
class FlowResource(private val uppercaseService: UppercaseService) {

    @ResponseStatus(201)
    @ResponseHeader(name = "foo", value = ["bar"])
    @GET
    @Path("str")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    fun sseStrings() = flow {
        emit(uppercaseService.convert("Hello"))
        emit(uppercaseService.convert("From"))
        emit(uppercaseService.convert("Kotlin"))
        emit(uppercaseService.convert("Flow"))
    }

    @GET
    @Path("suspendStr")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    suspend fun suspendSseStrings(): Flow<String> {
        delay(100)
        return flow {
            emit(uppercaseService.convert("Hello"))
            emit(uppercaseService.convert("From"))
            emit(uppercaseService.convert("Kotlin"))
            emit(uppercaseService.convert("Flow"))
        }
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
