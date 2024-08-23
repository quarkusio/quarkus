package org.acme

import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

import org.acme.service.SimpleService

@Path("/hello")
class ExampleResource {

    @Inject
    lateinit var service: SimpleService

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun hello(): String = service.hello()
}