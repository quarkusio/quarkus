package io.quarkus.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * This configuration class is here to avoid warnings when using {@code -Dquarkus.builder.=...}.
 *
 * @see io.quarkus.builder.BuildChainBuilder
 */
@ConfigRoot(name = "builder", phase = ConfigPhase.RUN_TIME)
public class BuilderConfig {

    /**
     * Dump the graph output to a file. This is useful for debugging.
     */
    @ConfigItem
    public Optional<String> graphOutput;

    /**
     * Whether to log the cause of a conflict.
     */
    @ConfigItem
    public Optional<Boolean> logConflictCause;

}
