package io.quarkus.it.optaplanner;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;

import io.quarkus.it.optaplanner.domain.ITestdataPlanningSolution;

@Path("/optaplanner/test")
public class OptaPlannerTestResource {

    @Inject
    SolverFactory<ITestdataPlanningSolution> solverFactory;

    @POST
    @Path("/solver-factory")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ITestdataPlanningSolution solveWithSolverFactory(ITestdataPlanningSolution problem) {
        Solver<ITestdataPlanningSolution> solver = solverFactory.buildSolver();
        return solver.solve(problem);
    }

}
