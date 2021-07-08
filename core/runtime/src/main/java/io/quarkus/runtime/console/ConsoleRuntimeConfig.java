package io.quarkus.runtime.console;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "console", phase = ConfigPhase.RUN_TIME)
public class ConsoleRuntimeConfig {

    /**
     * If color should be enabled or disabled.
     *
     * If this is not present then an attempt will be made to guess if the terminal supports color
     */
    @ConfigItem
    public Optional<Boolean> color;
}
