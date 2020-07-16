package io.quarkus.narayana.lra.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Configuration properties for controlling LRA coordinators
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public final class LRAConfiguration {
    /**
     * The node name used by the LRA coordinator to identify the logs that it owns.
     * The recommendation is to use a unique value for the node name
     */
    @ConfigItem(defaultValue = "quarkus")
    String nodeName;

    /**
     * The location of the LRA logs used by the LRA coordinator for logging and recovery.
     * In this version of the extension different coordinators must not share the same
     * log storage.
     */
    @ConfigItem(defaultValue = "lra-log-store")
    String storeDirectory;

    /**
     * The host on which a coordinator is running on. If the host and port are the
     * same as the quarkus host and port (quarkus.http.port) then the
     * application will run with an embedded coordinator. In order for an LRA
     * to end successfully and in order to join with an LRA the coordinator must
     * be available. In this version of the extension, a failed coordinator with
     * LRAs that have not yet finished must be restarted
     */
    @ConfigItem(defaultValue = "localhost")
    String coordinatorHost;

    /**
     * The port on which a coordinator is listening
     */
    @ConfigItem(defaultValue = "8080")
    int coordinatorPort;
}
