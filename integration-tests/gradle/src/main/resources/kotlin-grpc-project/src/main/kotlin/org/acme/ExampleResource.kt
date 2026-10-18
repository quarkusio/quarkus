package org.acme

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

import io.quarkus.example.HelloMsg
import io.quarkus.example.helloMsg

@Path("/hello")
class ExampleResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun hello() = "hello" + helloMsg { status = HelloMsg.Status.TEST_ONE }.statusValue
}