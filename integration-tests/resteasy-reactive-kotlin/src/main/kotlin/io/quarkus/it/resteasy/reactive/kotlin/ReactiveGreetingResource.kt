package io.quarkus.it.resteasy.reactive.kotlin

import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/hello-resteasy-reactive")
class ReactiveGreetingResource {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    suspend fun hello(): String {
        delay(50)
        doSomeWork()
        return "Hello RestEASY Reactive"
    }

    private suspend fun doSomeWork() {
        BufferedReader(
            InputStreamReader(
                URL("http://www.github.com")
                    .openConnection()
                    .getInputStream()))
            .lines()
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
        doSomeWork()
        return "Hello ${name}"
    }
}
