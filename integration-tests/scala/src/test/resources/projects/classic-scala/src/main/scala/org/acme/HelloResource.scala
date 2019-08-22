package org.acme

import javax.inject.Inject
import javax.ws.rs.{GET, Path, Produces}
import javax.ws.rs.core.MediaType
import org.eclipse.microprofile.config.inject.ConfigProperty

import scala.beans.BeanProperty

@Path("/hello")
class HelloResource {

  @Inject
  @ConfigProperty(name = "greeting")
  @BeanProperty
  var greeting: String = _

  @GET
  @Produces(Array[String](MediaType.TEXT_PLAIN))
  def hello(): String = "hello"

  @GET
  @Path("/greeting")
  @Produces(Array[String](MediaType.TEXT_PLAIN))
  def sayGreeting(): String = greeting
}
