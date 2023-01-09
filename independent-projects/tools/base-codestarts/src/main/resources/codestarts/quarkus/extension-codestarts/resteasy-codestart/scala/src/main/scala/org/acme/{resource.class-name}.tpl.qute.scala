package org.acme

import jakarta.ws.rs.\{GET, Path, Produces}
import jakarta.ws.rs.core.MediaType

@Path("{resource.path}")
class {resource.class-name} {

    @GET
    @Produces(Array[String](MediaType.TEXT_PLAIN))
    def hello() = "{resource.response}"
}