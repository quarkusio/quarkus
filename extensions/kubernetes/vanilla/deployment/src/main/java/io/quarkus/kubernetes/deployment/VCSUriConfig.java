package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.smallrye.config.WithDefault;

public interface VCSUriConfig {
    /**
     * Whether the vcs-uri annotation should be added to the generated configuration.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Optional override of the vcs-uri annotation.
     */
    Optional<String> override();
}
