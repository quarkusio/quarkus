package org.acme.logging.json

import org.jboss.logging.Logger
import org.jboss.resteasy.spi.NotImplementedYetException
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.ServerErrorException
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/logging-json")
class LoggingJsonExampleResource {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun hello(): String {
        LOG.info("Reached LoggingJsonExampleResource hello() method")
        return "hello"
    }

    @GET
    @Path("/goodbye")
    @Produces(MediaType.TEXT_PLAIN)
    fun goodbye(): String {
        LOG.info("Reached LoggingJsonExampleResource goodbye() method, about to throw an Exception")
        throw ServerErrorException(
                Response.Status.INTERNAL_SERVER_ERROR,
                NotImplementedYetException("goodbye() not implemented yet"))
    }

    companion object {
        private val LOG = Logger.getLogger(LoggingJsonExampleResource::class.java)
    }
}