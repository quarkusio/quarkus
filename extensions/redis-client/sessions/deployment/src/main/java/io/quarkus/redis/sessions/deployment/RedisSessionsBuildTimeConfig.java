package io.quarkus.redis.sessions.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Configuration of Vert.x Web sessions stored in Redis.
 */
@ConfigRoot(name = "http.sessions.redis", phase = ConfigPhase.BUILD_TIME)
public class RedisSessionsBuildTimeConfig {
    /**
     * Name of the Redis client configured in the Quarkus Redis extension configuration.
     * If not set, uses the default (unnamed) Redis client.
     */
    @ConfigItem
    public Optional<String> clientName;
}
