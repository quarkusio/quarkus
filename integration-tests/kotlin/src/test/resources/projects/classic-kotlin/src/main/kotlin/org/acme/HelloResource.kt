package org.acme

import org.eclipse.microprofile.config.inject.ConfigProperty

import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

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
