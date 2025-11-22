package io.quarkus.deployment.dev.testing;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * HTTP-related testing configuration.
 */
@ConfigMapping(prefix = "quarkus.http")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface HttpTestConfig {

    /**
     * The REST Assured client timeout for testing.
     */
    @WithDefault("30s")
    Duration testTimeout();
}
