package io.quarkus.amazon.lambda.deployment;

import io.smallrye.config.WithDefault;

/**
 * Configuration for the mock event server that is run
 * in dev mode and test mode
 */
public interface MockEventServerBuildConfig {
    /**
     * Setting to true will start event server even if quarkus.devservices.enabled=false
     */
    @WithDefault("true")
    boolean enabled();

}
