package io.quarkus.optaplanner;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.optaplanner.core.api.solver.SolverFactory;

import io.quarkus.arc.DefaultBean;

public class SolverFactoryProvider<Solution_> {

    static SolverFactory<?> solverFactory;

    @DefaultBean
    @Singleton
    @Produces
    SolverFactory<Solution_> solverFactory() {
        return (SolverFactory<Solution_>) solverFactory;
    }

}
