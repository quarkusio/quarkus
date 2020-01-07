package io.quarkus.optaplanner;

import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;

import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class OptaPlannerRecorder {

    public BeanContainerListener initialize(SolverConfig solverConfig) {
        return container -> {
            OptaPlannerBeanProvider.solverConfig = solverConfig;
            OptaPlannerBeanProvider.solverFactory = SolverFactory.create(solverConfig);
        };
    }

}
