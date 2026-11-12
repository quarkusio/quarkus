package io.quarkus.stork;

import java.time.Duration;
import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

/**
 * Stork configuration root.
 */
@ConfigMapping(prefix = "quarkus.stork")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface StorkConfiguration {

    /**
     * Configuration for the service
     */
    @WithParentName
    @ConfigDocSection
    @ConfigDocMapKey("service-name")
    Map<String, ServiceConfiguration> serviceConfiguration();

    /**
     * The maximum time to wait for a service instance to be registered when the application starts, and deregistered
     * when the application stops.
     * <p>
     * If the service registrar does not complete the operation in time, a warning is logged and the application
     * continues to start or stop.
     */
    @WithDefault("10s")
    Duration registrationTimeout();

}
