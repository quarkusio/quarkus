package com.andy

import javax.ws.rs.{GET, Path, Produces}
import javax.ws.rs.core.MediaType

@Path("/bonjour")
class BonjourResource {

    @GET
    @Produces(Array[String](MediaType.TEXT_PLAIN))
    def hello() = "Hello RESTEasy"
}