package ${package_name}

import javax.ws.rs.{GET, Path, Produces}
import javax.ws.rs.core.MediaType

@Path("${path}")
class ${class_name} {

    @GET
    @Produces(Array[String](MediaType.TEXT_PLAIN))
    def hello() = "hello"
}
