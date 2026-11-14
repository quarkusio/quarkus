package io.quarkus.http.ratelimit.redis.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.http.rate-limit")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface HttpRateLimitRedisBuildTimeConfig {
    /**
     * The name of the {@code quarkus.redis.<name>.*} client the {@code redis} store uses. The default Redis client when
     * absent. Only this client is requested from the Redis client extension.
     */
    @WithName("redis.client-name")
    Optional<String> redisClientName();
}
