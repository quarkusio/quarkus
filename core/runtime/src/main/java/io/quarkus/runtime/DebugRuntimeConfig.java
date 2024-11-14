package io.quarkus.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Debugging.
 */
@ConfigMapping(prefix = "quarkus.debug")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface DebugRuntimeConfig {

    /**
     * If set to {@code true}, Quarkus prints the wall-clock time each build step took to complete.
     * This is useful as a first step in debugging slow startup times.
     */
    @WithDefault("false")
    boolean printStartupTimes();
}
