package io.quarkus.optaplanner;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.optaplanner.core.api.score.ScoreManager;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.SolverManagerConfig;

import io.quarkus.arc.DefaultBean;

public class OptaPlannerBeanProvider {

    @DefaultBean
    @Singleton
    @Produces
    <Solution_> SolverFactory<Solution_> solverFactory(SolverConfig solverConfig) {
        return SolverFactory.create(solverConfig);
    }

    @DefaultBean
    @Singleton
    @Produces
    <Solution_, ProblemId_> SolverManager<Solution_, ProblemId_> solverManager(SolverFactory<Solution_> solverFactory,
            SolverManagerConfig solverManagerConfig) {
        return SolverManager.create(solverFactory, solverManagerConfig);
    }

    @DefaultBean
    @Singleton
    @Produces
    <Solution_> ScoreManager<Solution_> scoreManager(SolverFactory<Solution_> solverFactory) {
        return ScoreManager.create(solverFactory);
    }

}
