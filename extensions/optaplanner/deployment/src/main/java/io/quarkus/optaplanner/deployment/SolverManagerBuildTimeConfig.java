package io.quarkus.optaplanner.deployment;

import java.util.Optional;

import org.optaplanner.core.config.solver.SolverManagerConfig;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * During build time, this is translated into OptaPlanner's {@link SolverManagerConfig}.
 */
@ConfigGroup
public class SolverManagerBuildTimeConfig {

    /**
     * The number of solvers that run in parallel. This directly influences CPU consumption.
     * Defaults to {@value SolverManagerConfig#PARALLEL_SOLVER_COUNT_AUTO}.
     * Other options include a number or formula based on the available processor count.
     */
    @ConfigItem
    Optional<String> parallelSolverCount;

}
