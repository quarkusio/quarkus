package io.quarkus.optaplanner.deployment;

import java.util.Optional;

import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.SolverManagerConfig;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * During build time, this is translated into OptaPlanner's Config classes.
 */
@ConfigRoot(name = "optaplanner")
public class OptaPlannerBuildTimeConfig {

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
    SolverBuildTimeConfig solver;
    /**
     * Configuration properties that overwrite OptaPlanner's {@link SolverManagerConfig}.
     */
    @ConfigItem
    SolverManagerBuildTimeConfig solverManager;

}
