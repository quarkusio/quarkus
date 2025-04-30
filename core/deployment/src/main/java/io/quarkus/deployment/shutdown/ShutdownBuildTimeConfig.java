package io.quarkus.deployment.shutdown;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Shutdown
 */
@ConfigMapping(prefix = "quarkus.shutdown")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface ShutdownBuildTimeConfig {

    /**
     * Whether Quarkus should wait between shutdown being requested and actually initiated.
     * This delay gives the infrastructure time to detect that the application instance is shutting down and
     * stop routing traffic to it.
     */
    @WithDefault("false")
    boolean delayEnabled();

    default boolean isDelayEnabled() {
        return delayEnabled() && LaunchMode.current() != LaunchMode.DEVELOPMENT;
    }
}
