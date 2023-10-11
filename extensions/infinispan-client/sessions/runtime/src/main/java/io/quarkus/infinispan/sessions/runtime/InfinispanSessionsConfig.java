package io.quarkus.infinispan.sessions.runtime;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Configuration of Vert.x Web sessions stored in remote Infinispan cache.
 */
@ConfigRoot(name = "http.sessions.infinispan", phase = ConfigPhase.RUN_TIME)
public class InfinispanSessionsConfig {
    /**
     * Name of the Infinispan cache used to store session data. If it does not exist, it is created
     * automatically from Infinispan's default template {@code DIST_SYNC}.
     */
    @ConfigItem(defaultValue = "quarkus.sessions")
    public String cacheName;

    /**
     * Maximum time to retry when retrieving session data from the Infinispan cache.
     * The Vert.x session handler retries when the session data are not found, because
     * distributing data across an Infinispan cluster may take time.
     */
    @ConfigItem(defaultValue = "5s")
    public Duration retryTimeout;
}
