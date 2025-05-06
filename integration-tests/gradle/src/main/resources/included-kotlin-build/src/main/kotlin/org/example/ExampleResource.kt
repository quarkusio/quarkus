package org.example

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/hello")
class ExampleResource(val someBean: SomeBean) {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun hello() = someBean.someMethod()
}