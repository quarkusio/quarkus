package ilove.quark.us

import jakarta.ws.rs.{GET, Path, Produces}
import jakarta.ws.rs.core.MediaType

@Path("/hello")
class GreetingResource {

    @GET
    @Produces(Array[String](MediaType.TEXT_PLAIN))
    def hello() = "Hello RESTEasy"
}