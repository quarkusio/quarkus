package org.acme

import javax.ws.rs.\{GET, Path, Produces}
import javax.ws.rs.core.MediaType

@Path("{resource.path}")
class {resource.class-name} {

    @GET
    @Produces(Array[String](MediaType.TEXT_PLAIN))
    def hello() = "{resource.response}"
}