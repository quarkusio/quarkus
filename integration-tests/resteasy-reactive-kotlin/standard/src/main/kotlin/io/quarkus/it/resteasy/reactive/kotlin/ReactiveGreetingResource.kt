package io.quarkus.it.resteasy.reactive.kotlin

import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/hello-resteasy-reactive")
class ReactiveGreetingResource @Inject constructor (val req : RequestScopedKotlinClass){
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    suspend fun hello(): String {
        req.message = "msg"
        delay(50)
        if (req.message != "msg") {
            throw Throwable("Request scoped data was lost");
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
        return "Hello ${name}"
    }
}
