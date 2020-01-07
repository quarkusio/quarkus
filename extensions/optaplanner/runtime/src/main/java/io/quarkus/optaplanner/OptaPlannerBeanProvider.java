package io.quarkus.optaplanner;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;

import io.quarkus.arc.DefaultBean;

public class OptaPlannerBeanProvider<Solution_> {

    static SolverConfig solverConfig;
    static SolverFactory<?> solverFactory;

    @DefaultBean
    @Singleton
    @Produces
    SolverConfig solverConfig() {
        return solverConfig;
    }

    @DefaultBean
    @Singleton
    @Produces
    SolverFactory<Solution_> solverFactory() {
        return (SolverFactory<Solution_>) solverFactory;
    }

}
