package io.quarkus.deployment.shutdown;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Shutdown
 */
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public class ShutdownBuildTimeConfig {

    /**
     * Whether Quarkus should wait between shutdown being requested and actually initiated.
     * This delay gives the infrastructure time to detect that the application instance is shutting down and
     * stop routing traffic to it.
     */
    @ConfigItem(defaultValue = "false")
    public boolean delayEnabled;
}
