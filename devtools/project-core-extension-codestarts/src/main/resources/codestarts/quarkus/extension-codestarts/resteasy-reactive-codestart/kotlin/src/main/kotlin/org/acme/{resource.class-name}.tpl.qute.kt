package org.acme

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("{resource.path}")
class {resource.class-name} {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun hello() = "{resource.response}"
}