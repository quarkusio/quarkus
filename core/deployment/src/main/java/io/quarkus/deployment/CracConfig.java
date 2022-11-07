package io.quarkus.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class CracConfig {

    /**
     * Enable/Disable CRAC integration
     * <p>
     * Default value is dependent on extensions deployed
     * (i.e. when using AWS Lambda extensions, this will be set to true by default)
     */
    @ConfigItem
    Optional<Boolean> enable;

    /**
     * Will do a classpath search for all META-INF/quarkus-preload-classes.txt files
     * These files contain fully qualified classnames that should be loaded
     * in the CRAC`beforeCheckpoint()` phase
     */
    @ConfigItem(defaultValue = "true")
    boolean preloadClasses;

    /**
     * if preloading classes, specify whether or not
     * to do static initialization when preloading these classes.
     */
    @ConfigItem(defaultValue = "true")
    boolean initializeClasses;

    /**
     * Perform Application.start() within CRAC `beforeCheckpoint()` phase.
     */
    @ConfigItem(defaultValue = "true")
    boolean fullWarmup;

    /**
     * When CRAC is enabled, it generates the application class list so it can be preloaded.
     */
    @ConfigItem(defaultValue = "true")
    boolean generateApplicationClassList;

}
