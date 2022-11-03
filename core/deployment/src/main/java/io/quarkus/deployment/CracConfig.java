package io.quarkus.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class CracConfig {

    /**
     * Enable/Disable CRAC registration
     * Default value is dependent on extensions deployed
     * (i.e. with Lambda this will be set to true by default)
     */
    @ConfigItem
    Optional<Boolean> enable;

    /**
     * Will do a classpath search for all META-INF/crac-preload-classes files
     * These files contain fully qualified classnames that should be loaded
     * in the CRAC init phase
     *
     */
    @ConfigItem(defaultValue = "true")
    boolean preloadClasses;

    /**
     * Perform Application.start() within CRAC INIT phase (beforeCheckpoint())
     */
    @ConfigItem(defaultValue = "false")
    boolean fullWarmup;
}
