package io.quarkus.runtime;

import java.time.Instant;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class BuildConfig {

    /**
     * The build timestamp.
     */
    @ConfigItem
    public Optional<Instant> timestamp;
}
