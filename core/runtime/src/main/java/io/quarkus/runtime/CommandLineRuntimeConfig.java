package io.quarkus.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * This configuration class is here to avoid warnings when using {@code -Dquarkus.args=...}.
 */
@ConfigRoot(name = ConfigItem.PARENT, phase = ConfigPhase.RUN_TIME)
public class CommandLineRuntimeConfig {

    /**
     * The arguments passed to the command line.
     * <p>
     * We don't make it a list as the args are separated by a space, not a comma.
     */
    @ConfigItem
    public Optional<String> args;

}
