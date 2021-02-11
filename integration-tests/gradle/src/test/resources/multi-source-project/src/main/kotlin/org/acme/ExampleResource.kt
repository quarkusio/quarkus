package org.acme

import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

import org.acme.service.SimpleService

@Path("/hello")
class ExampleResource {

    @Inject
    lateinit var service: SimpleService

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun hello(): String = service.hello()
}