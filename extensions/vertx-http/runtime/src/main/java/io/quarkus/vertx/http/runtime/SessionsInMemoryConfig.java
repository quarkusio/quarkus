package io.quarkus.vertx.http.runtime;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Configuration of Vert.x Web sessions stored in memory.
 */
@ConfigRoot(name = "http.sessions.in-memory", phase = ConfigPhase.RUN_TIME)
public class SessionsInMemoryConfig {
    /**
     * Name of the Vert.x local map or cluster-wide map to store the session data.
     */
    @ConfigItem(defaultValue = "quarkus.sessions")
    public String mapName;

    /**
     * Whether in-memory sessions are stored cluster-wide.
     * <p>
     * Ignored when Vert.x clustering is not enabled.
     */
    @ConfigItem(defaultValue = "false")
    public boolean clusterWide;

    /**
     * Maximum time to retry when retrieving session data from the cluster-wide map.
     * The Vert.x session handler retries when the session data are not found, because
     * distributing data across the cluster may take time.
     * <p>
     * Ignored when in-memory sessions are not cluster-wide.
     */
    @ConfigItem(defaultValue = "5s")
    public Duration retryTimeout;
}
