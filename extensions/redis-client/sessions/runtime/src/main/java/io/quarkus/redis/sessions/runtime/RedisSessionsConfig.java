package io.quarkus.redis.sessions.runtime;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Configuration of Vert.x Web sessions stored in Redis.
 */
@ConfigRoot(name = "http.sessions.redis", phase = ConfigPhase.RUN_TIME)
public class RedisSessionsConfig {
    /**
     * Maximum time to retry when retrieving session data from the Redis server.
     * The Vert.x session handler retries when the session data are not found, because
     * distributing data across a potential Redis cluster may take some time.
     */
    @ConfigItem(defaultValue = "2s")
    public Duration retryTimeout;
}
