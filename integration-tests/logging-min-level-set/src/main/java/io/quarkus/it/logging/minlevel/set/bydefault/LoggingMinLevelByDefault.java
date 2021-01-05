package io.quarkus.it.logging.minlevel.set.bydefault;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

import io.quarkus.it.logging.minlevel.set.LoggingWitness;

@Path("/log/bydefault")
public class LoggingMinLevelByDefault {

    static final Logger LOG = Logger.getLogger(LoggingMinLevelByDefault.class);

    @GET
    @Path("/debug")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean isDebug() {
        return LOG.isDebugEnabled() && LoggingWitness.loggedDebug("debug message", LOG);
    }

    @GET
    @Path("/not-trace")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean isNotTrace() {
        return !LOG.isTraceEnabled() && !LoggingWitness.loggedTrace("should not print", LOG);
    }

}
