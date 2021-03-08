package org.acme


import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/app/hello")
class HelloResource() {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun hello4() = getAGreetingFailure().getOrDefault("hello")

    fun getAGreetingFailure(): Result<String> = kotlin.runCatching {
        throw Error("Something else 2")
    }
}
