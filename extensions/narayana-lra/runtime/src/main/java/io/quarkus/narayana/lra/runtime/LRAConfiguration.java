package io.quarkus.narayana.lra.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration properties for controlling LRA participants
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.lra")
public interface LRAConfiguration {
    /**
     * The REST endpoint on which a coordinator is running.
     * In order for an LRA to begin and end successfully and in order to
     * join with an existing LRA, this coordinator must be available
     * whenever a service method annotated with @LRA is invoked.
     *
     * In this version of the extension, a failed coordinator with
     * LRAs that have not yet finished must be restarted.
     */
    @WithDefault("http://localhost:50000/lra-coordinator")
    String coordinatorURL();
}
