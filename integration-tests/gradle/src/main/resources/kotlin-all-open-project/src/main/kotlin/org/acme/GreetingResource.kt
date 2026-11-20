package org.acme

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import kotlinx.coroutines.runBlocking

@Path("/hello")
class GreetingResource(private val service: GreetingService) {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun hello(): String = runBlocking { service.greet() }
}
