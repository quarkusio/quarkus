package io.quarkus.it.logging.minlevel.set.filter;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

import io.quarkus.it.logging.minlevel.set.LoggingWitness;

@Path("/log/filter")
public class LoggingFilter {

    static final Logger LOG = Logger.getLogger(LoggingFilter.class);

    @GET
    @Path("/filtered")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean filtered() {
        return LoggingWitness.loggedWarn("TEST warn message", LOG);
    }

    @GET
    @Path("/not-filtered")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean notFiltered() {
        return LoggingWitness.loggedWarn("warn message", LOG);
    }

}
