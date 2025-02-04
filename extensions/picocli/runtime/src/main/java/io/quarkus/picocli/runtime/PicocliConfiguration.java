package io.quarkus.picocli.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.picocli")
public interface PicocliConfiguration {
    /**
     * Name of bean annotated with {@link io.quarkus.picocli.runtime.annotations.TopCommand}
     * or FQCN of class which will be used as entry point for Picocli CommandLine instance.
     * This class needs to be annotated with {@link picocli.CommandLine.Command}.
     */
    Optional<String> topCommand();
}
