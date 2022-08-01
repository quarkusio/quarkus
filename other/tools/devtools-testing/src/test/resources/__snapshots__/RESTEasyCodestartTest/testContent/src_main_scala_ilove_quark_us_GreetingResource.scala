package ilove.quark.us

import javax.ws.rs.{GET, Path, Produces}
import javax.ws.rs.core.MediaType

@Path("/hello")
class GreetingResource {

    @GET
    @Produces(Array[String](MediaType.TEXT_PLAIN))
    def hello() = "Hello RESTEasy"
}