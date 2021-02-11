package org.acme.example

import java.util.*
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/my-hello-example")
class MyHelloExample {

    @GET
    @Produces(MediaType.TEXT_HTML)
    fun hello(): String {
        return "My Example Hello Quarkus Codestart"
    }
}
