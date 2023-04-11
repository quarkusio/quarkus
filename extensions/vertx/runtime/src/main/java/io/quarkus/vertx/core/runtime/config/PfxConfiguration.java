package io.quarkus.vertx.core.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigGroup
public interface PfxConfiguration {

    /**
     * PFX config is disabled by default.
     */
    @WithParentName
    @WithDefault("false")
    boolean enabled();

    /**
     * Path to the key file (PFX format).
     */
    Optional<String> path();

    /**
     * Password of the key.
     */
    Optional<String> password();
}
