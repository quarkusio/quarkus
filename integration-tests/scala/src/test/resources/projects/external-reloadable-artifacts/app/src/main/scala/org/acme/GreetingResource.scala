package org.acme

import javax.ws.rs.{GET, Path, Produces}
import javax.ws.rs.core.MediaType

import org.acme.lib.Greeting

@Path("/hello")
class GreetingResource {

  @GET
  @Produces(Array[String](MediaType.TEXT_PLAIN))
  def hello(): String = Greeting.hello()

}
