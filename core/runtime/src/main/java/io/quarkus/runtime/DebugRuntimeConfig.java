package io.quarkus.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Debugging.
 */
@ConfigRoot(name = "debug", phase = ConfigPhase.RUN_TIME)
public class DebugRuntimeConfig {

    /**
     * If set to {@code true}, Quarkus prints the wall-clock time each build step took to complete.
     * This is useful as a first step in debugging slow startup times.
     */
    @ConfigItem(defaultValue = "false")
    boolean printStartupTimes;
}
