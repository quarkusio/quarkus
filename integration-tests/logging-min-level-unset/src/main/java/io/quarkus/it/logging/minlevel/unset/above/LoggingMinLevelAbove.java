package io.quarkus.it.logging.minlevel.unset.above;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

import io.quarkus.it.logging.minlevel.unset.LoggingWitness;

@Path("/log/above")
public class LoggingMinLevelAbove {

    static final Logger LOG = Logger.getLogger(LoggingMinLevelAbove.class);

    @GET
    @Path("/not-info")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean isNotInfo() {
        return !LOG.isInfoEnabled() && LoggingWitness.notLoggedInfo("should not print", LOG);
    }

    @GET
    @Path("/warn")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean isWarn() {
        return LoggingWitness.loggedWarn("warn message", LOG);
    }

    @GET
    @Path("/not-trace")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean isNotTrace() {
        return !LOG.isTraceEnabled() && LoggingWitness.notLoggedTrace("should not print", LOG);
    }

}
