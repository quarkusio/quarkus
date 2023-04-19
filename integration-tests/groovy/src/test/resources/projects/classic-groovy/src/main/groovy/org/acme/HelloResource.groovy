package org.acme

import org.eclipse.microprofile.config.inject.ConfigProperty

import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/hello")
class HelloResource {

    @Inject
    GreetingService greetingService
    @Inject
    @ConfigProperty(name = "greeting")
    String greeting

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    def hello() {
        'hello'
    }

    @GET
    @Path("/greeting")
    @Produces(MediaType.TEXT_PLAIN)
    def greeting() {
        greeting
    }

    @GET
    @Path("/bean")
    @Produces(MediaType.TEXT_PLAIN)
    def greetingFromBean() {
        greetingService.greet()
    }
}
