package org.acme


import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/app/hello")
class HelloResource() {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun hello4() = getAGreetingFailure().getOrDefault("hello")

    fun getAGreetingFailure(): Result<String> = kotlin.runCatching {
        throw Error("Something else 2")
    }
}