package com.example.quarkusmm

import com.example.quarkusmm.port.CustomerService
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/hello")
class ExampleResource {

    @Inject
    lateinit var service: CustomerService

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun hello() = service.getMessage()
}