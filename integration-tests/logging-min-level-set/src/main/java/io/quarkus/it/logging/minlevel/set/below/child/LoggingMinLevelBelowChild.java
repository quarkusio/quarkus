package io.quarkus.it.logging.minlevel.set.below.child;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

import io.quarkus.it.logging.minlevel.set.LoggingWitness;

@Path("/log/below/child")
public class LoggingMinLevelBelowChild {

    static final Logger LOG = Logger.getLogger(LoggingMinLevelBelowChild.class);

    @GET
    @Path("/trace")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean isTrace() {
        return LOG.isTraceEnabled() && LoggingWitness.loggedTrace("trace-message", LOG);
    }

}
