package io.quarkus.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Application
 */
@ConfigMapping(prefix = "quarkus.application")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface ApplicationConfig {

    /**
     * The name of the application.
     * If not set, defaults to the name of the project (except for tests where it is not set at all).
     */
    Optional<String> name();

    /**
     * The version of the application.
     * If not set, defaults to the version of the project (except for tests where it is not set at all).
     */
    Optional<String> version();

    /**
     * The header to use for UI Screen (Swagger UI, GraphQL UI etc).
     */
    @WithDefault("{applicationName} (powered by Quarkus)")
    Optional<String> uiHeader();
}
