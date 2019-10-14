package ${package_name}

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("${path}")
class ${class_name} {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun hello() = "hello"
}
