package io.quarkus.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * SnapStart
 * <p>
 * Configure the various optimization to use
 * <a href="https://docs.aws.amazon.com/lambda/latest/dg/snapstart.html">SnapStart</a>
 */
@ConfigMapping(prefix = "quarkus.snapstart")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface SnapStartConfig {

    /**
     * Enable/Disable SnapStart integration
     * <p>
     * Default value is dependent on extensions deployed
     * (i.e. when using AWS Lambda extensions, this will be set to true by default)
     */
    Optional<Boolean> enable();

    /**
     * Will do a classpath search for all {@code META-INF/quarkus-preload-classes.txt} files
     * These files contain fully qualified classnames that should be loaded in the SnapStart/CRaC
     * {@code beforeCheckpoint()} phase.
     */
    @WithDefault("true")
    boolean preloadClasses();

    /**
     * if preloading classes, specify whether to do static initialization when preloading these classes.
     */
    @WithDefault("true")
    boolean initializeClasses();

    /**
     * Start the full application during the snapshotting process.
     * In other words, when enabled, it performs {@code Application.start()} within SnapStart/CRaC
     * {@code beforeCheckpoint()} phase.
     */
    @WithDefault("true")
    boolean fullWarmup();

    /**
     * When SnapStart is enabled, it generates the application class list, so it can be preloaded.
     * Only used if {@link #preloadClasses} is set to {@code true}.
     */
    @WithDefault("true")
    boolean generateApplicationClassList();

}
