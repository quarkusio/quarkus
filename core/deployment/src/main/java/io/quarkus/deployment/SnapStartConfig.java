package io.quarkus.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * SnapStart
 * <p>
 * Configure the various optimization to use
 * <a href="https://docs.aws.amazon.com/lambda/latest/dg/snapstart.html">SnapStart</a>
 */
@ConfigRoot(phase = ConfigPhase.BUILD_TIME, name = "snapstart")
public class SnapStartConfig {

    /**
     * Enable/Disable SnapStart integration
     * <p>
     * Default value is dependent on extensions deployed
     * (i.e. when using AWS Lambda extensions, this will be set to true by default)
     */
    @ConfigItem
    Optional<Boolean> enable;

    /**
     * Will do a classpath search for all {@code META-INF/quarkus-preload-classes.txt} files
     * These files contain fully qualified classnames that should be loaded in the SnapStart/CRaC
     * {@code beforeCheckpoint()} phase.
     */
    @ConfigItem(defaultValue = "true")
    boolean preloadClasses;

    /**
     * if preloading classes, specify whether to do static initialization when preloading these classes.
     */
    @ConfigItem(defaultValue = "true")
    boolean initializeClasses;

    /**
     * Start the full application during the snapshotting process.
     * In other words, when enabled, it performs {@code Application.start()} within SnapStart/CRaC
     * {@code beforeCheckpoint()} phase.
     */
    @ConfigItem(defaultValue = "true")
    boolean fullWarmup;

    /**
     * When SnapStart is enabled, it generates the application class list, so it can be preloaded.
     * Only used if {@link #preloadClasses} is set to {@code true}.
     */
    @ConfigItem(defaultValue = "true")
    boolean generateApplicationClassList;

}
