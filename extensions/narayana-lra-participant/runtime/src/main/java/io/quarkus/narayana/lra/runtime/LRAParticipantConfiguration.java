package io.quarkus.narayana.lra.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Configuration properties for controlling LRA coordinators
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public final class LRAParticipantConfiguration {
    /**
     * The host on which a coordinator is running
     */
    @ConfigItem(defaultValue = "localhost")
    String coordinatorHost;

    /**
     * The port on which a coordinator is listening
     */
    @ConfigItem(defaultValue = "8080")
    int coordinatorPort;
}
