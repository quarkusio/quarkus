package io.quarkus.optaplanner.rest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.optaplanner.core.config.solver.SolverConfig;

@Path("/solver-config")
@ApplicationScoped
public class SolverConfigTestResource {

    @Inject
    SolverConfig solverConfig;

    @GET
    @Path("/seconds-spent-limit")
    @Produces(MediaType.TEXT_PLAIN)
    public String secondsSpentLimit() {
        return "secondsSpentLimit=" + solverConfig.getTerminationConfig().getSecondsSpentLimit();
    }

}
