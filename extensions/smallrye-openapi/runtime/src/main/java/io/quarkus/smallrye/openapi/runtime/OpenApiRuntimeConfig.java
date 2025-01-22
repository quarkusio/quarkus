package io.quarkus.smallrye.openapi.runtime;

import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.smallrye-openapi")
public interface OpenApiRuntimeConfig {

    /**
     * Enable the openapi endpoint. By default it's enabled.
     */
    @WithDefault("true")
    boolean enable();

    /**
     * Specify the list of global servers that provide connectivity information
     */
    Optional<Set<String>> servers();
}
