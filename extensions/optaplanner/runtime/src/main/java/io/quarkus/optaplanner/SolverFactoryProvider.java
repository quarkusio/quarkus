package io.quarkus.optaplanner;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.quarkus.arc.DefaultBean;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;

public class SolverFactoryProvider<Solution_> {

    static SolverFactory<?> solverFactory;

    @DefaultBean
    @Singleton
    @Produces
    SolverFactory<Solution_> solverFactory() {
        return (SolverFactory<Solution_>) solverFactory;
    }

}
