package org.acme.logging.json;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.NotImplementedYetException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/logging-json")
public class LoggingJsonExampleResource {

    private static final Logger LOG = Logger.getLogger(LoggingJsonExampleResource.class);

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        LOG.info("Reached LoggingJsonExampleResource hello() method");
        return "hello";
    }

    @GET
    @Path("/goodbye")
    @Produces(MediaType.TEXT_PLAIN)
    public String goodbye() {
        LOG.info("Reached LoggingJsonExampleResource goodbye() method, about to throw an Exception");
        throw new ServerErrorException(
                Response.Status.INTERNAL_SERVER_ERROR,
                new NotImplementedYetException("goodbye() not implemented yet"));
    }
}