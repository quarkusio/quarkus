package org.acme

import jakarta.inject.Inject
import jakarta.ws.rs.{GET, Path, Produces}
import jakarta.ws.rs.core.MediaType
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
