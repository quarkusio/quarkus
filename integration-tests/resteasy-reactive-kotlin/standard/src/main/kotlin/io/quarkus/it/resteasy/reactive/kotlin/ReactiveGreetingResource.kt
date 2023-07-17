package io.quarkus.it.resteasy.reactive.kotlin

import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import kotlinx.coroutines.delay

@Path("/hello-resteasy-reactive")
class ReactiveGreetingResource @Inject constructor(val req: RequestScopedKotlinClass) {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    suspend fun hello(): String {
        req.message = "msg"
        delay(50)
        if (req.message != "msg") {
            throw Throwable("Request scoped data was lost")
        }
        return "Hello RestEASY Reactive"
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/standard")
    fun standard(): String {
        return "Hello RestEASY Reactive"
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{name}")
    suspend fun hello(name: String): String {
        delay(50)
        return "Hello $name"
    }
}
