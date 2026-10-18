package io.quarkus.narayana.lra.runtime;

import java.util.Optional;

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
     * <p>
     * In this version of the extension, a failed coordinator with
     * LRAs that have not yet finished must be restarted.
     */
    @WithDefault("http://localhost:50000/lra-coordinator")
    String coordinatorURL();

    /**
     * The base URI override for this participant service. This is useful when
     * the service runs behind a reverse proxy or load balancer, and the
     * coordinator can bypass the proxy with direct access to the service.
     * <p>
     * The coordinator will use this base URI to call the participant service
     * to append complete, compensate, status, etc. endpoints.
     */
    Optional<String> baseUri();
}
