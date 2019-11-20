package io.quarkus.optaplanner;

import java.util.List;

import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.ConstraintStreamImplType;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.score.director.ScoreDirectorFactoryConfig;
import org.optaplanner.core.config.solver.SolverConfig;

import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class OptaPlannerRecorder {

    public BeanContainerListener initializeSolverFactory(SolverConfig solverConfig) {
        return container -> {
            SolverFactoryProvider.solverFactory = SolverFactory.create(solverConfig);
        };
    }

}
