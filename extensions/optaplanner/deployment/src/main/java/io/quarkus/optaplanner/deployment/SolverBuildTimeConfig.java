package io.quarkus.optaplanner.deployment;

import java.util.Optional;

import org.optaplanner.core.config.solver.EnvironmentMode;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * During build time, this is translated into OptaPlanner's {@link SolverConfig}.
 */
@ConfigGroup
public class SolverBuildTimeConfig {

    public static final String DEFAULT_SCORE_DRL_URL = "constraints.drl";

    /**
     * Enable runtime assertions to detect common bugs in your implementation during development.
     * Defaults to {@link EnvironmentMode#REPRODUCIBLE}.
     */
    @ConfigItem
    Optional<EnvironmentMode> environmentMode;
    /**
     * Enable multithreaded solving for a single problem, which increases CPU consumption.
     * Defaults to {@value SolverConfig#MOVE_THREAD_COUNT_NONE}.
     * Other options include {@value SolverConfig#MOVE_THREAD_COUNT_AUTO}, a number
     * or formula based on the available processor count.
     */
    @ConfigItem
    Optional<String> moveThreadCount;

    /**
     * Configuration properties that overwrite OptaPlanner's {@link TerminationConfig}.
     */
    @ConfigItem
    TerminationBuildTimeConfig termination;

}
