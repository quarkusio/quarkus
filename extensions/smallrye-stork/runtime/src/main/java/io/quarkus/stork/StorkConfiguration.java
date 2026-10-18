package io.quarkus.stork;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
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

}
