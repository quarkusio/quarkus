package io.quarkus.it.logging.minlevel.unset.bydefault;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

import io.quarkus.it.logging.minlevel.unset.LoggingWitness;

@Path("/log/bydefault")
public class LoggingMinLevelByDefault {

    static final Logger LOG = Logger.getLogger(LoggingMinLevelByDefault.class);

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
