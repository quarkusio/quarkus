
package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class TrafficConfig {
    /**
     * Tag is optionally used to expose a dedicated url for referencing
     * this target exclusively.
     */
    @ConfigItem
    Optional<String> tag;

    /**
     * RevisionName of a specific revision to which to send this portion of traffic.
     */
    @ConfigItem
    Optional<String> revisionName;

    /**
     * LatestRevision may be optionally provided to indicate that the latest
     * ready Revision of the Configuration should be used for this traffic
     * target. When provided LatestRevision must be true if RevisionName is
     * empty.
     */
    @ConfigItem(defaultValue = "false")
    Optional<Boolean> latestRevision;

    /**
     * Percent indicates that percentage based routing should be used and
     * the value indicates the percent of traffic that is be routed to this
     * Revision or Configuration. `0` (zero) mean no traffic, `100` means all
     * traffic.
     */
    @ConfigItem(defaultValue = "100")
    Optional<Long> percent;
}
