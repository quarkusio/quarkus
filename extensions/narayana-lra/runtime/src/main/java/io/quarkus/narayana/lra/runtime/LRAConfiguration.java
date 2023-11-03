package io.quarkus.narayana.lra.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Configuration properties for controlling LRA participants
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public final class LRAConfiguration {
    /**
     * The REST endpoint on which a coordinator is running.
     * In order for an LRA to begin and end successfully and in order to
     * join with an existing LRA, this coordinator must be available
     * whenever a service method annotated with @LRA is invoked.
     *
     * In this version of the extension, a failed coordinator with
     * LRAs that have not yet finished must be restarted.
     */
    @ConfigItem(defaultValue = "http://localhost:50000/lra-coordinator")
    String coordinatorURL;
}
