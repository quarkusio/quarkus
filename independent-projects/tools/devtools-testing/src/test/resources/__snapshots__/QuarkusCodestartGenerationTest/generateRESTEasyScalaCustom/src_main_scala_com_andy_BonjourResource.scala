package com.andy

import jakarta.ws.rs.{GET, Path, Produces}
import jakarta.ws.rs.core.MediaType

@Path("/bonjour")
class BonjourResource {

    @GET
    @Produces(Array[String](MediaType.TEXT_PLAIN))
    def hello() = "Hello RESTEasy"
}