package org.example

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/hello")
class ExampleResource() {

    @GET
    @Path("/kmp")
    @Produces(MediaType.TEXT_PLAIN)
    fun helloKmp() = SomeObject.aString

    @GET
    @Path("/jvm")
    @Produces(MediaType.TEXT_PLAIN)
    fun helloJvm() = AnotherObject.aString
}