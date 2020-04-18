package io.quarkus.optaplanner;

import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.SolverManagerConfig;

import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class OptaPlannerRecorder {

    public BeanContainerListener initialize(SolverConfig solverConfig, SolverManagerConfig solverManagerConfig) {
        return container -> {
            // Don't use the Quarkus build-time classloader at runtime
            solverConfig.setClassLoader(
                    solverConfig.getSolutionClass() == null ? null : solverConfig.getSolutionClass().getClassLoader());
            OptaPlannerBeanProvider.solverConfig = solverConfig;
            OptaPlannerBeanProvider.solverManagerConfig = solverManagerConfig;
        };
    }

}
