package com.example.quarkusmm

import com.example.quarkusmm.port.CustomerService
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/hello")
class ExampleResource {

    @Inject
    lateinit var service: CustomerService

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun hello() = service.getMessage()
}