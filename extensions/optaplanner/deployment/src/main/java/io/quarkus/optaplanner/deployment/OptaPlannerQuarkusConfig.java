package io.quarkus.optaplanner.deployment;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import org.optaplanner.core.config.solver.EnvironmentMode;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.SolverManagerConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;

@ConfigRoot(name = "optaplanner")
public class OptaPlannerQuarkusConfig {

    public static final String DEFAULT_SOLVER_CONFIG_URL = "solverConfig.xml";

    /**
     * A classpath resource to read the solver configuration XML.
     * Defaults to {@value DEFAULT_SOLVER_CONFIG_URL}.
     * If this property isn't specified, that solverConfig.xml is optional.
     */
    @ConfigItem
    Optional<String> solverConfigXml;

    /**
     * Configuration properties that overwrite OptaPlanner's {@link SolverConfig}.
     */
    @ConfigItem
    SolverQuarkusConfig solver;
    /**
     * Configuration properties that overwrite OptaPlanner's {@link SolverManagerConfig}.
     */
    @ConfigItem
    SolverManagerQuarkusConfig solverManager;

    /**
     * Subset of OptaPlanner's {@link SolverConfig}.
     */
    @ConfigGroup
    public static class SolverQuarkusConfig {

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
        TerminationQuarkusConfig termination;

    }

    /**
     * Subset of OptaPlanner's {@link TerminationConfig}.
     */
    @ConfigGroup
    public static class TerminationQuarkusConfig {

        /**
         * How long the solver can run.
         * For example: "30s" is 30 seconds. "5m" is 5 minutes. "2h" is 2 hours. "1d" is 1 day.
         * Also supports ISO-8601 format, see {@link Duration}.
         */
        @ConfigItem
        Optional<Duration> spentLimit;
        /**
         * How long the solver can run without finding a new best solution after finding a new best solution.
         * For example: "30s" is 30 seconds. "5m" is 5 minutes. "2h" is 2 hours. "1d" is 1 day.
         * Also supports ISO-8601 format, see {@link Duration}.
         */
        @ConfigItem
        Optional<Duration> unimprovedSpentLimit;
        /**
         * Terminates the solver when a specific or higher score has been reached.
         * For example: "0hard/-1000soft" terminates when the best score changes from "0hard/-1200soft" to "0hard/-900soft".
         * Wildcards are supported to replace numbers.
         * For example: "0hard/*soft" to terminate when any feasible score is reached.
         */
        @ConfigItem
        Optional<String> bestScoreLimit;

    }

    /**
     * Subset of OptaPlanner's {@link SolverManagerConfig}.
     */
    @ConfigGroup
    public static class SolverManagerQuarkusConfig {

        /**
         * The number of solvers that run in parallel. This directly influences CPU consumption.
         * Defaults to {@value SolverManagerConfig#PARALLEL_SOLVER_COUNT_AUTO}.
         * Other options include a number or formula based on the available processor count.
         */
        @ConfigItem
        Optional<String> parallelSolverCount;

    }

}
