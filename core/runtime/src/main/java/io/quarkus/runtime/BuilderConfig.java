package io.quarkus.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Builder.
 * <p>
 * This configuration class is here to avoid warnings when using {@code -Dquarkus.builder.=...}.
 *
 * @see io.quarkus.builder.BuildChainBuilder
 */
@ConfigMapping(prefix = "quarkus.builder")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface BuilderConfig {

    /**
     * Dump the graph output to a file. This is useful for debugging.
     */
    Optional<String> graphOutput();

    /**
     * Whether to log the cause of a conflict.
     */
    Optional<Boolean> logConflictCause();

    /**
     * Build metrics configuration.
     */
    Metrics Metrics();

    interface Metrics {

        /**
         * If set to true then dump the build metrics to a JSON file in the build directory.
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * If set to true then the collection of metrics is enhanced but the size of the generated JSON file may grow
         * significantly.
         */
        @WithDefault("false")
        boolean extendedCapture();

    }
}
