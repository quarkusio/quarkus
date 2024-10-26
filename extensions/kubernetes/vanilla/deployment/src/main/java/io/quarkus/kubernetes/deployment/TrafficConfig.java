package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.smallrye.config.WithDefault;

public interface TrafficConfig {
    /**
     * Tag is optionally used to expose a dedicated url for referencing
     * this target exclusively.
     */
    Optional<String> tag();

    /**
     * RevisionName of a specific revision to which to send this portion of traffic.
     */
    Optional<String> revisionName();

    /**
     * LatestRevision may be optionally provided to indicate that the latest
     * ready Revision of the Configuration should be used for this traffic
     * target. When provided LatestRevision must be true if RevisionName is
     * empty.
     */
    @WithDefault("false")
    Optional<Boolean> latestRevision();

    /**
     * Percent indicates that percentage based routing should be used and
     * the value indicates the percent of traffic that is to be routed to this
     * Revision or Configuration. `0` (zero) mean no traffic, `100` means all
     * traffic.
     */
    @WithDefault("100")
    Optional<Long> percent();
}
