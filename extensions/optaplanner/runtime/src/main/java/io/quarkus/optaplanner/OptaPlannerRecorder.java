package io.quarkus.optaplanner;

import java.util.function.Supplier;

import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.SolverManagerConfig;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class OptaPlannerRecorder {

    public Supplier<SolverConfig> solverConfigSupplier(final SolverConfig solverConfig) {
        return new Supplier<SolverConfig>() {
            @Override
            public SolverConfig get() {
                return solverConfig;
            }
        };
    }

    public Supplier<SolverManagerConfig> solverManagerConfig(final SolverManagerConfig solverManagerConfig) {
        return new Supplier<SolverManagerConfig>() {
            @Override
            public SolverManagerConfig get() {
                return solverManagerConfig;
            }
        };
    }

}
