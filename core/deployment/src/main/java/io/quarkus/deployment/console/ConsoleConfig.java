package io.quarkus.deployment.console;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocIgnore;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Console
 */
@ConfigMapping(prefix = "quarkus.console")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface ConsoleConfig {
    /**
     * If test results and status should be displayed in the console.
     * <p>
     * If this is false results can still be viewed in the dev console.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Disables the ability to enter input on the console.
     */
    @WithDefault("false")
    boolean disableInput();

    /**
     * Disable the testing status/prompt message at the bottom of the console
     * and log these messages to STDOUT instead.
     * <p>
     * Use this option if your terminal does not support ANSI escape sequences.
     */
    @WithDefault("false")
    boolean basic();

    /**
     * If color should be enabled or disabled.
     * <p>
     * If this is not present then an attempt will be made to guess if the terminal supports color
     */
    @ConfigDocIgnore
    Optional<Boolean> color();
}
