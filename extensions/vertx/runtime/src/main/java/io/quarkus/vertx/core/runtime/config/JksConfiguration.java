package io.quarkus.vertx.core.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

@ConfigGroup
public interface JksConfiguration {

    /**
     * JKS config is disabled by default.
     */
    @WithParentName
    @WithDefault("false")
    boolean enabled();

    /**
     * Path of the key file (JKS format).
     */
    Optional<String> path();

    /**
     * Password of the key file.
     */
    Optional<String> password();
}
