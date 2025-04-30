package io.quarkus.amazon.lambda.deployment;

import io.smallrye.config.WithDefault;

/**
 * Configuration for the mock event server that is run
 * in dev mode and test mode
 */
public interface MockEventServerConfig {
    /**
     * Setting to true will start event server even if quarkus.devservices.enabled=false
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Port to access mock event server in dev mode
     */
    @WithDefault("8080")
    int devPort();

    /**
     * Port to access mock event server in dev mode
     */
    @WithDefault("8081")
    int testPort();
}
