package io.quarkus.it.logging.minlevel.unset.promote;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

import io.quarkus.it.logging.minlevel.unset.LoggingWitness;

@Path("/log/promote")
public class LoggingMinLevelPromote {
    static final Logger LOG = Logger.getLogger(LoggingMinLevelPromote.class);

    @GET
    @Path("/info")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean isInfo() {
        return LOG.isInfoEnabled() && LoggingWitness.loggedInfo("info message", LOG);
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
