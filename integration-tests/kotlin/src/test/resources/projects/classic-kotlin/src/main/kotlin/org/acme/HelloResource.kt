package org.acme

import org.eclipse.microprofile.config.inject.ConfigProperty

import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/hello")
class HelloResource(val greetingService: GreetingService) {

    @Inject
    @ConfigProperty(name = "greeting")
    internal var greeting: String? = null

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun hello(): String {
        return "hello"
    }

    @GET
    @Path("/greeting")
    @Produces(MediaType.TEXT_PLAIN)
    fun greeting(): String? {
        return greeting
    }

    @GET
    @Path("/bean")
    @Produces(MediaType.TEXT_PLAIN)
    fun greetingFromBean()  = greetingService.greet()
}
