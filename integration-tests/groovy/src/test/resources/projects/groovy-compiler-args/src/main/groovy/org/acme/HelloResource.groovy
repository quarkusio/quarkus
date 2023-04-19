package org.acme


import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/app/hello")
class HelloResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    def hello4() {
        'hello'
    }
}