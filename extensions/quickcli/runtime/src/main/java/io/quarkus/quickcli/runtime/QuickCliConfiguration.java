package io.quarkus.quickcli.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.quickcli")
public interface QuickCliConfiguration {
    /**
     * Name of bean annotated with {@link io.quarkus.quickcli.runtime.annotations.TopCommand}
     * or FQCN of class which will be used as entry point for QuickCLI CommandLine instance.
     * This class needs to be annotated with {@link io.quarkus.quickcli.annotations.Command}.
     */
    Optional<String> topCommand();
}
