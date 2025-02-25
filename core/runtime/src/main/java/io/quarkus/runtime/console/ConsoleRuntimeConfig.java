package io.quarkus.runtime.console;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * Console
 */
@ConfigMapping(prefix = "quarkus.console")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ConsoleRuntimeConfig {
    /**
     * If color should be enabled or disabled.
     * <p>
     * If this is not present then an attempt will be made to guess if the terminal supports color
     */
    Optional<Boolean> color();
}
