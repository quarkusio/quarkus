package io.quarkus.runtime;

import java.time.Instant;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class ApplicationConfig {

    /**
     * The name of the application.
     * If not set, defaults to the name of the project (except for tests where it is not set at all).
     */
    @ConfigItem
    public Optional<String> name;

    /**
     * The version of the application.
     * If not set, defaults to the version of the project (except for tests where it is not set at all).
     */
    @ConfigItem
    public Optional<String> version;

    /**
     * An {@link Instant} to use as a build time. It does not necessarily have to be the clock time at the time when the
     * application is built. This can also be some stable value for the sake of reproducibility.
     */
    @ConfigItem
    public Optional<Instant> buildTime;

}
