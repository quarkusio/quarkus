package io.quarkus.it.logging.minlevel.set.below;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

import io.quarkus.it.logging.minlevel.set.LoggingWitness;

@Path("/log/below")
public class LoggingMinLevelBelow {

    static final Logger LOG = Logger.getLogger(LoggingMinLevelBelow.class);

    @GET
    @Path("/trace")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean isTrace() {
        return LOG.isTraceEnabled() && LoggingWitness.loggedTrace("trace-message", LOG);
    }

}
