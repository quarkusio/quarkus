package {package-name}

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("{resource.path}")
class {resource.class-name} {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun hello() = "{resource.response}"
}